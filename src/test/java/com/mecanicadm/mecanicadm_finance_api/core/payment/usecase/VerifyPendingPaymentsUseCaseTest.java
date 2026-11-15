package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.VerifyPendingPaymentsCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VerifyPendingPaymentsUseCaseTest {

    private final PaymentGateway paymentGateway = mock(PaymentGateway.class);
    private final MercadoPagoOrderGateway orderGateway = mock(MercadoPagoOrderGateway.class);
    private final MercadoPagoAccessTokenProvider tokenProvider = mock(MercadoPagoAccessTokenProvider.class);
    private final PaymentOrderEventService eventService = mock(PaymentOrderEventService.class);
    private final VerifyPendingPaymentsUseCase useCase =
            new VerifyPendingPaymentsUseCase(paymentGateway, orderGateway, tokenProvider, eventService);
    private final VerifyPendingPaymentsCommand command = new VerifyPendingPaymentsCommand(Duration.ofMinutes(1), 20);
    private final UUID workOrderId = UUID.randomUUID();

    private MercadoPagoOrder order(String orderStatus, String paymentStatus) throws Exception {
        return new ObjectMapper().readValue("{\"id\":\"ORD1\",\"status\":\"" + orderStatus + "\",\"transactions\":{\"payments\":[{\"id\":\"PAY1\",\"status\":\""
                + paymentStatus + "\"}]}}", MercadoPagoOrder.class);
    }

    private Payment pending() throws Exception {
        return new Payment(UUID.randomUUID(), workOrderId.toString(), workOrderId.toString(), "ORD1", "PAY1", "action_required", null,
                new BigDecimal("720.00"), 1, "visa", "a@a.com", order("action_required", "action_required"), Instant.now(), Instant.now());
    }

    private void givenPending(Payment... payments) {
        when(paymentGateway.findPendingForVerification(any(), anyInt())).thenReturn(List.of(payments));
        when(tokenProvider.accessToken()).thenReturn("token");
        when(paymentGateway.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Deve atualizar o pagamento e publicar o resultado quando o Mercado Pago o confirmar")
    void shouldPublishOutcomeWhenPaymentIsConfirmed() throws Exception {
        Payment payment = pending();
        givenPending(payment);
        when(orderGateway.getOrder("ORD1", "token")).thenReturn(order("processed", "processed"));

        int changed = useCase.execute(command);

        assertThat(changed).isEqualTo(1);
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentGateway).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo("processed");
        verify(eventService).publishOutcome(saved.getValue(), workOrderId);
    }

    @Test
    @DisplayName("Deve publicar o resultado quando o Mercado Pago recusar o pagamento")
    void shouldPublishOutcomeWhenPaymentIsRejected() throws Exception {
        givenPending(pending());
        when(orderGateway.getOrder("ORD1", "token")).thenReturn(order("failed", "rejected"));

        assertThat(useCase.execute(command)).isEqualTo(1);

        verify(eventService).publishOutcome(any(Payment.class), eq(workOrderId));
    }

    @Test
    @DisplayName("Sem mudanca, deve so renovar a data de verificacao, sem publicar evento")
    void shouldOnlyTouchPaymentWhenNothingChanged() throws Exception {
        givenPending(pending());
        when(orderGateway.getOrder("ORD1", "token")).thenReturn(order("action_required", "action_required"));

        assertThat(useCase.execute(command)).isZero();

        verify(paymentGateway).save(any(Payment.class));
        verify(eventService, never()).publishOutcome(any(), any());
    }

    @Test
    @DisplayName("Falha ao consultar o Mercado Pago nao deve alterar o pagamento nem impedir os demais")
    void shouldSkipPaymentWhenProviderFails() throws Exception {
        Payment broken = pending();
        Payment healthy = new Payment(UUID.randomUUID(), workOrderId.toString(), workOrderId.toString(), "ORD2", "PAY2", "action_required", null,
                BigDecimal.TEN, 1, "visa", "a@a.com", order("action_required", "action_required"), Instant.now(), Instant.now());
        givenPending(broken, healthy);
        when(orderGateway.getOrder("ORD1", "token")).thenThrow(new PaymentException.MercadoPagoProviderError(502, "fora"));
        when(orderGateway.getOrder("ORD2", "token")).thenReturn(order("processed", "processed"));

        assertThat(useCase.execute(command)).isEqualTo(1);

        verify(paymentGateway).save(any(Payment.class));
    }

    @Test
    @DisplayName("Sem pagamentos pendentes, nao deve pedir token nem consultar o Mercado Pago")
    void shouldDoNothingWhenThereIsNothingPending() {
        when(paymentGateway.findPendingForVerification(any(), anyInt())).thenReturn(List.of());

        assertThat(useCase.execute(command)).isZero();

        verifyNoInteractions(tokenProvider, orderGateway, eventService);
    }

    @Test
    @DisplayName("Deve buscar os pendentes mais antigos que a idade minima, no tamanho de lote informado")
    void shouldQueryByMinAgeAndBatchSize() {
        when(paymentGateway.findPendingForVerification(any(), anyInt())).thenReturn(List.of());
        Instant before = Instant.now().minus(Duration.ofMinutes(1));

        useCase.execute(command);

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(paymentGateway).findPendingForVerification(cutoff.capture(), eq(20));
        assertThat(cutoff.getValue()).isBetween(before.minusSeconds(5), before.plusSeconds(5));
    }
}
