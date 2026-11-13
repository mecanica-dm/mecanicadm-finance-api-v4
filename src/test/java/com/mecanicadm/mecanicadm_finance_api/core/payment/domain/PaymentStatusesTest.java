package com.mecanicadm.mecanicadm_finance_api.core.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusesTest {

    @Test
    @DisplayName("Deve reconhecer status confirmado, cancelado e estornado sem diferenciar maiusculas")
    void shouldClassifyStatuses() {
        assertThat(PaymentStatuses.isConfirmed("PROCESSED")).isTrue();
        assertThat(PaymentStatuses.isConfirmed("approved")).isTrue();
        assertThat(PaymentStatuses.isConfirmed("action_required")).isFalse();
        assertThat(PaymentStatuses.isConfirmed(null)).isFalse();
        assertThat(PaymentStatuses.isCanceled("Canceled")).isTrue();
        assertThat(PaymentStatuses.isCanceled("cancelled")).isTrue();
        assertThat(PaymentStatuses.isCanceled("processed")).isFalse();
        assertThat(PaymentStatuses.isRefunded("REFUNDED")).isTrue();
        assertThat(PaymentStatuses.isRefunded(null)).isFalse();
    }



    @Test
    @DisplayName("Estados finais devem incluir confirmados, recusados, cancelados e estornados, e nao os pendentes")
    void shouldKnowFinalStatuses() {
        assertThat(PaymentStatuses.FINAL).contains("processed", "approved", "failed", "rejected", "canceled", "expired", "refunded");
        assertThat(PaymentStatuses.FINAL).doesNotContain("created", "action_required", "pending");
    }

    @Test
    @DisplayName("O estado efetivo deve ser o do primeiro pagamento da resposta do provedor e, sem ele, o do registro")
    void shouldResolveEffectiveStatus() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        MercadoPagoOrder withPayment = mapper.readValue("{\"status\":\"action_required\",\"transactions\":{\"payments\":[{\"status\":\"PROCESSED\"}]}}", MercadoPagoOrder.class);
        MercadoPagoOrder withoutPayment = mapper.readValue("{\"status\":\"created\"}", MercadoPagoOrder.class);
        java.time.Instant now = java.time.Instant.now();

        Payment a = new Payment(java.util.UUID.randomUUID(), "r", "k", "o", null, "created", null, java.math.BigDecimal.TEN, 1, "visa", "a@a", withPayment, now, now);
        Payment b = new Payment(java.util.UUID.randomUUID(), "r", "k", "o", null, "Created", null, java.math.BigDecimal.TEN, 1, "visa", "a@a", withoutPayment, now, now);
        Payment c = new Payment(java.util.UUID.randomUUID(), "r", "k", "o", null, null, null, java.math.BigDecimal.TEN, 1, "visa", "a@a", null, now, now);

        assertThat(PaymentStatuses.effective(a)).isEqualTo("processed");
        assertThat(PaymentStatuses.effective(b)).isEqualTo("created");
        assertThat(PaymentStatuses.effective(c)).isEmpty();
    }
}
