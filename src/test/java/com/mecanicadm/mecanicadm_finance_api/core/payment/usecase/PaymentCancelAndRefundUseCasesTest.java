package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentCanceledEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentRefundedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CancelMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.RefundMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCancelAndRefundUseCasesTest {

    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private MercadoPagoOrderGateway orderGateway;
    @Mock
    private MercadoPagoAccessTokenProvider accessTokenProvider;
    @Mock
    private EventPublisher eventPublisher;

    private CancelMercadoPagoPaymentUseCase cancel;
    private RefundMercadoPagoPaymentUseCase refund;
    private final UUID workOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cancel = new CancelMercadoPagoPaymentUseCase(paymentGateway, orderGateway, accessTokenProvider, eventPublisher);
        refund = new RefundMercadoPagoPaymentUseCase(paymentGateway, orderGateway, accessTokenProvider, eventPublisher);
    }

    private Payment payment(String status) {
        return new Payment(UUID.randomUUID(), workOrderId.toString(), workOrderId.toString(), "ORD1", "PAY1", status, null,
                new BigDecimal("720.00"), 1, "visa", "a@a.com", null, Instant.now(), Instant.now());
    }

    private void found(Payment payment) {
        when(paymentGateway.findLatestByExternalReference(workOrderId.toString())).thenReturn(Optional.of(payment));
    }

    private MessageHeader published() {
        ArgumentCaptor<MessageHeader> captor = ArgumentCaptor.forClass(MessageHeader.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Cancelar: deve cancelar o pedido no Mercado Pago, marcar o pagamento como cancelado e publicar PaymentCanceledEvent")
    void shouldCancelPendingPayment() {
        Payment payment = payment("action_required");
        found(payment);
        when(accessTokenProvider.accessToken()).thenReturn("token");
        when(orderGateway.cancelOrder("ORD1", "token", "cancel-" + payment.id())).thenReturn(new MercadoPagoOrder());
        when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        cancel.execute(new CancelMercadoPagoPaymentCommand(workOrderId));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentGateway).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo("canceled");
        assertThat(published()).isInstanceOf(PaymentCanceledEvent.class)
                .extracting(MessageHeader::workOrderId).isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Cancelar: 409 order_already_canceled do Mercado Pago deve ser tratado como cancelamento concluido")
    void shouldTreatAlreadyCanceledAtProviderAsDone() {
        Payment payment = payment("action_required");
        found(payment);
        when(accessTokenProvider.accessToken()).thenReturn("token");
        when(orderGateway.cancelOrder(anyString(), anyString(), anyString()))
                .thenThrow(new PaymentException.MercadoPagoProviderError(409, "{\"errors\":[{\"code\":\"order_already_canceled\"}]}"));
        when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        cancel.execute(new CancelMercadoPagoPaymentCommand(workOrderId));

        assertThat(published()).isInstanceOf(PaymentCanceledEvent.class);
    }

    @Test
    @DisplayName("Cancelar: outro erro 409 do Mercado Pago (ex.: cannot_cancel_order) deve ser propagado")
    void shouldPropagateOtherProviderErrors() {
        found(payment("action_required"));
        when(accessTokenProvider.accessToken()).thenReturn("token");
        when(orderGateway.cancelOrder(anyString(), anyString(), anyString()))
                .thenThrow(new PaymentException.MercadoPagoProviderError(409, "cannot_cancel_order"));

        assertThatThrownBy(() -> cancel.execute(new CancelMercadoPagoPaymentCommand(workOrderId)))
                .isInstanceOf(PaymentException.MercadoPagoProviderError.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Cancelar: sem pagamento (link ainda nao gerado) deve so confirmar o cancelamento")
    void shouldConfirmCancellationWhenThereIsNoPayment() {
        when(paymentGateway.findLatestByExternalReference(workOrderId.toString())).thenReturn(Optional.empty());

        cancel.execute(new CancelMercadoPagoPaymentCommand(workOrderId));

        verifyNoInteractions(orderGateway);
        assertThat(published()).isInstanceOf(PaymentCanceledEvent.class);
    }

    @Test
    @DisplayName("Cancelar: pagamento ja cancelado deve republicar a confirmacao sem chamar o Mercado Pago")
    void shouldBeIdempotentWhenAlreadyCanceled() {
        found(payment("canceled"));

        cancel.execute(new CancelMercadoPagoPaymentCommand(workOrderId));

        verifyNoInteractions(orderGateway);
        verify(paymentGateway, never()).save(any());
        assertThat(published()).isInstanceOf(PaymentCanceledEvent.class);
    }

    @Test
    @DisplayName("Cancelar: pagamento ja confirmado deve ser recusado, pois so pode ser estornado")
    void shouldRefuseToCancelConfirmedPayment() {
        found(payment("processed"));

        assertThatThrownBy(() -> cancel.execute(new CancelMercadoPagoPaymentCommand(workOrderId)))
                .isInstanceOf(PaymentException.PaymentAlreadyConfirmed.class);

        verifyNoInteractions(orderGateway, eventPublisher);
    }

    @Test
    @DisplayName("Estornar: deve estornar o pagamento confirmado, marcar como estornado e publicar PaymentRefundedEvent")
    void shouldRefundConfirmedPayment() {
        Payment payment = payment("processed");
        found(payment);
        when(accessTokenProvider.accessToken()).thenReturn("token");
        when(orderGateway.refundOrder("ORD1", "token", "refund-" + payment.id())).thenReturn(new MercadoPagoOrder());
        when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        refund.execute(new RefundMercadoPagoPaymentCommand(workOrderId));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentGateway).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo("refunded");
        MessageHeader event = published();
        assertThat(event).isInstanceOf(PaymentRefundedEvent.class);
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Estornar: pagamento ja estornado deve republicar a confirmacao sem chamar o Mercado Pago")
    void shouldBeIdempotentWhenAlreadyRefunded() {
        found(payment("refunded"));

        refund.execute(new RefundMercadoPagoPaymentCommand(workOrderId));

        verify(orderGateway, never()).refundOrder(anyString(), anyString(), anyString());
        assertThat(published()).isInstanceOf(PaymentRefundedEvent.class);
    }

    @Test
    @DisplayName("Estornar: sem pagamento ou com pagamento nao confirmado deve recusar")
    void shouldRefuseToRefundWhenThereIsNothingToRefund() {
        when(paymentGateway.findLatestByExternalReference(workOrderId.toString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> refund.execute(new RefundMercadoPagoPaymentCommand(workOrderId)))
                .isInstanceOf(PaymentException.PaymentNotFound.class);

        found(payment("action_required"));
        assertThatThrownBy(() -> refund.execute(new RefundMercadoPagoPaymentCommand(workOrderId)))
                .isInstanceOf(PaymentException.PaymentNotRefundable.class);

        verifyNoInteractions(orderGateway, eventPublisher);
    }
}
