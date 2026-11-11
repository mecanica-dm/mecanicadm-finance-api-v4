package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentConfirmedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentFailedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentLinkGenerationFailedEvent;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.PaymentLinkGeneratedEvent;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentOrderEventServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    private PaymentOrderEventService service;
    private UUID workOrderId;

    @BeforeEach
    void setUp() {
        service = new PaymentOrderEventService(eventPublisher);
        workOrderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve publicar evento de link gerado com os dados do pagamento")
    void shouldPublishLinkGeneratedEvent() {
        Payment payment = payment("approved", "accredited", "https://mercadopago/ticket");

        service.publishLinkGenerated(payment, workOrderId);

        ArgumentCaptor<PaymentLinkGeneratedEvent> captor =
                ArgumentCaptor.forClass(PaymentLinkGeneratedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        PaymentLinkGeneratedEvent event = captor.getValue();
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.paymentId()).isEqualTo(payment.id());
        assertThat(event.paymentLink()).isEqualTo("https://mercadopago/ticket");
        assertThat(event.amount()).isEqualByComparingTo(payment.amount());
        assertThat(event.messageId()).isNotNull();
    }

    @Test
    @DisplayName("Deve publicar confirmação quando o status do pagamento for aprovado")
    void shouldPublishConfirmedEvent() {
        Payment payment = payment("approved", "accredited", null);

        service.publishOutcome(payment, workOrderId);

        ArgumentCaptor<PaymentConfirmedEvent> captor =
                ArgumentCaptor.forClass(PaymentConfirmedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        PaymentConfirmedEvent event = captor.getValue();
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.paymentId()).isEqualTo(payment.id());
        assertThat(event.mercadoPagoPaymentId()).isEqualTo(payment.mercadoPagoPaymentId());
        assertThat(event.amount()).isEqualByComparingTo(payment.amount());
    }

    @Test
    @DisplayName("Deve publicar falha quando o status do pagamento for rejeitado")
    void shouldPublishFailedEvent() {
        Payment payment = payment("rejected", "cc_rejected_other_reason", null);

        service.publishOutcome(payment, workOrderId);

        ArgumentCaptor<PaymentFailedEvent> captor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        PaymentFailedEvent event = captor.getValue();
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.paymentId()).isEqualTo(payment.id());
        assertThat(event.reason()).isEqualTo("cc_rejected_other_reason");
    }

    @Test
    @DisplayName("Não deve publicar desfecho quando o status do pagamento for desconhecido")
    void shouldNotPublishOutcomeForUnknownStatus() {
        service.publishOutcome(payment("pending", null, null), workOrderId);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve publicar falha na geração do link")
    void shouldPublishLinkGenerationFailedEvent() {
        String externalReference = UUID.randomUUID().toString();

        service.publishLinkGenerationFailed(workOrderId, externalReference, "provider unavailable");

        ArgumentCaptor<PaymentLinkGenerationFailedEvent> captor =
                ArgumentCaptor.forClass(PaymentLinkGenerationFailedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        PaymentLinkGenerationFailedEvent event = captor.getValue();
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.externalReference()).isEqualTo(externalReference);
        assertThat(event.reason()).isEqualTo("provider unavailable");
        assertThat(event.messageId()).isNotNull();
    }

    private Payment payment(String orderStatus, String statusDetail, String ticketUrl) {
        MercadoPagoOrder.PaymentMethod paymentMethod = new MercadoPagoOrder.PaymentMethod();
        paymentMethod.setId("visa");
        paymentMethod.setTicketUrl(ticketUrl);

        MercadoPagoOrder.Payment providerPayment = new MercadoPagoOrder.Payment();
        providerPayment.setId("mp-payment-1");
        providerPayment.setStatus(orderStatus);
        providerPayment.setStatusDetail(statusDetail);
        providerPayment.setPaymentMethod(paymentMethod);

        MercadoPagoOrder.Transactions transactions = new MercadoPagoOrder.Transactions();
        transactions.setPayments(List.of(providerPayment));

        MercadoPagoOrder order = new MercadoPagoOrder();
        order.setId("mp-order-1");
        order.setStatus(orderStatus);
        order.setStatusDetail(statusDetail);
        order.setTransactions(transactions);

        Instant now = Instant.now();
        return new Payment(
                UUID.randomUUID(),
                "external-reference",
                "idempotency-key",
                order.getId(),
                providerPayment.getId(),
                orderStatus,
                statusDetail,
                new BigDecimal("50.00"),
                1,
                "visa",
                "payer@example.com",
                order,
                now,
                now
        );
    }
}
