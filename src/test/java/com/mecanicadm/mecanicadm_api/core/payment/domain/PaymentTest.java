package com.mecanicadm.mecanicadm_api.core.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentTest {

    @Test
    @DisplayName("Deve derivar os dados do provedor ao criar um pagamento")
    void shouldDeriveProviderDataWhenCreatingPayment() {
        MercadoPagoOrder.PaymentMethod paymentMethod = new MercadoPagoOrder.PaymentMethod();
        paymentMethod.setId("master");

        MercadoPagoOrder.Payment providerPayment = new MercadoPagoOrder.Payment();
        providerPayment.setId("mp-payment-1");
        providerPayment.setPaymentMethod(paymentMethod);

        MercadoPagoOrder.Transactions transactions = new MercadoPagoOrder.Transactions();
        transactions.setPayments(List.of(providerPayment));

        MercadoPagoOrder order = new MercadoPagoOrder();
        order.setId("mp-order-1");
        order.setStatus("approved");
        order.setStatusDetail("accredited");
        order.setTransactions(transactions);

        Payment payment = Payment.create(
                "external-reference",
                "idempotency-key",
                new BigDecimal("50.00"),
                3,
                "visa",
                "payer@example.com",
                order
        );

        assertNotNull(payment.id());
        assertEquals("external-reference", payment.externalReference());
        assertEquals("idempotency-key", payment.idempotencyKey());
        assertEquals("mp-order-1", payment.mercadoPagoOrderId());
        assertEquals("mp-payment-1", payment.mercadoPagoPaymentId());
        assertEquals("approved", payment.status());
        assertEquals("accredited", payment.statusDetail());
        assertEquals(new BigDecimal("50.00"), payment.amount());
        assertEquals(3, payment.installments());
        assertEquals("master", payment.paymentMethodId());
        assertEquals("payer@example.com", payment.payerEmail());
        assertEquals(order, payment.providerResponse());
        assertNotNull(payment.createdAt());
        assertNotNull(payment.updatedAt());
    }

    @Test
    @DisplayName("Deve usar o método de pagamento solicitado quando o provedor não informar")
    void shouldUseRequestedPaymentMethodWhenProviderDoesNotInformIt() {
        MercadoPagoOrder order = new MercadoPagoOrder();
        order.setId("mp-order-1");

        Payment payment = Payment.create(
                "external-reference",
                "idempotency-key",
                new BigDecimal("50.00"),
                1,
                "visa",
                "payer@example.com",
                order
        );

        assertEquals("visa", payment.paymentMethodId());
        assertNull(payment.mercadoPagoPaymentId());
    }

    @Test
    @DisplayName("Deve usar status desconhecido quando o provedor não informar status")
    void shouldUseUnknownStatusWhenProviderDoesNotInformIt() {
        Payment payment = Payment.create(
                "external-reference",
                "idempotency-key",
                new BigDecimal("50.00"),
                1,
                "visa",
                "payer@example.com",
                null
        );

        assertEquals("UNKNOWN", payment.status());
        assertNull(payment.mercadoPagoOrderId());
        assertNull(payment.providerResponse());
    }
}
