package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMercadoPagoPaymentUseCaseTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Test
    @DisplayName("Deve retornar a resposta do Mercado Pago persistida")
    void shouldReturnStoredProviderResponse() {
        GetMercadoPagoPaymentUseCase useCase = new GetMercadoPagoPaymentUseCase(paymentGateway);
        UUID id = UUID.randomUUID();
        MercadoPagoOrder order = new MercadoPagoOrder();
        Payment payment = Payment.create(
                "external-reference", UUID.randomUUID().toString(), null, 1, "visa", "payer@example.com", order);

        when(paymentGateway.findById(id)).thenReturn(Optional.of(payment));

        assertSame(payment, useCase.execute(id));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o pagamento não existir")
    void shouldThrowWhenPaymentDoesNotExist() {
        GetMercadoPagoPaymentUseCase useCase = new GetMercadoPagoPaymentUseCase(paymentGateway);
        UUID id = UUID.randomUUID();

        when(paymentGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(PaymentException.PaymentNotFound.class, () -> useCase.execute(id));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o pagamento não tiver resposta do provedor")
    void shouldThrowWhenProviderResponseIsMissing() {
        GetMercadoPagoPaymentUseCase useCase = new GetMercadoPagoPaymentUseCase(paymentGateway);
        UUID id = UUID.randomUUID();
        Payment payment = Payment.create(
                "external-reference", UUID.randomUUID().toString(), null, 1, "visa", "payer@example.com", null);

        when(paymentGateway.findById(id)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.PaymentNotFound.class, () -> useCase.execute(id));
    }
}
