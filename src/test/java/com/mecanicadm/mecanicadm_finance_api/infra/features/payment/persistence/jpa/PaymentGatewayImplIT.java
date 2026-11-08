package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentGatewayImplIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanPayments() {
        jdbcTemplate.update("DELETE FROM payments");
    }

    @Test
    @DisplayName("Deve persistir e recuperar o pagamento com a resposta do Mercado Pago")
    void shouldPersistAndRetrievePayment() {
        String idempotencyKey = UUID.randomUUID().toString();
        Payment payment = Payment.create(
                "external-reference",
                idempotencyKey,
                new BigDecimal("50.00"),
                3,
                "visa",
                "test_payer@example.com",
                order()
        );

        paymentGateway.save(payment);

        Payment byIdempotencyKey = paymentGateway.findByIdempotencyKey(idempotencyKey).orElseThrow();
        Payment byId = paymentGateway.findById(byIdempotencyKey.id()).orElseThrow();

        assertThat(byId.idempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(byId.externalReference()).isEqualTo("external-reference");
        assertThat(byId.mercadoPagoOrderId()).isEqualTo("mp-order-1");
        assertThat(byId.mercadoPagoPaymentId()).isEqualTo("mp-payment-1");
        assertThat(byId.status()).isEqualTo("approved");
        assertThat(byId.statusDetail()).isEqualTo("accredited");
        assertThat(byId.amount()).isEqualByComparingTo("50.00");
        assertThat(byId.installments()).isEqualTo(3);
        assertThat(byId.paymentMethodId()).isEqualTo("visa");
        assertThat(byId.payerEmail()).isEqualTo("test_payer@example.com");
        assertThat(byId.providerResponse()).isNotNull();
        assertThat(byId.providerResponse().getId()).isEqualTo("mp-order-1");
        assertThat(byId.providerResponse().getTransactions().getPayments()).hasSize(1);
        assertThat(byId.providerResponse().getTransactions().getPayments().getFirst().getId())
                .isEqualTo("mp-payment-1");
    }

    @Test
    @DisplayName("Deve retornar o pagamento existente ao salvar uma chave de idempotência duplicada")
    void shouldReturnExistingPaymentWhenIdempotencyKeyIsDuplicated() {
        String idempotencyKey = UUID.randomUUID().toString();
        Payment first = Payment.create(
                "external-reference",
                idempotencyKey,
                new BigDecimal("50.00"),
                1,
                "visa",
                "test_payer@example.com",
                order()
        );
        Payment duplicated = Payment.create(
                "other-reference",
                idempotencyKey,
                new BigDecimal("99.00"),
                1,
                "visa",
                "other_payer@example.com",
                order()
        );

        paymentGateway.save(first);
        Payment result = paymentGateway.save(duplicated);

        assertThat(result.id()).isEqualTo(first.id());
        assertThat(result.externalReference()).isEqualTo("external-reference");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Integer.class)).isEqualTo(1);
    }

    private MercadoPagoOrder order() {
        MercadoPagoOrder.PaymentMethod paymentMethod = new MercadoPagoOrder.PaymentMethod();
        paymentMethod.setId("visa");

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
        return order;
    }
}
