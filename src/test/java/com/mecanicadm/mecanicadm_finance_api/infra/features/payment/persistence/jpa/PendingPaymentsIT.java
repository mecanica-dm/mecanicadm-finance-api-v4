package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PendingPaymentsIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentGateway gateway;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM payments");
    }

    private UUID payment(String status, String orderId, Instant updatedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO payments (id, external_reference, idempotency_key, mercado_pago_order_id, status, amount, installments, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 10, 1, ?)", id, id.toString(), id.toString(), orderId, status, java.sql.Timestamp.from(updatedAt));
        return id;
    }

    private List<UUID> pending(Instant before) {
        return gateway.findPendingForVerification(before, 50).stream().map(Payment::id).toList();
    }

    @Test
    @DisplayName("Deve listar so os pagamentos em estado nao final, com pedido no Mercado Pago, e ja sem verificar ha um tempo")
    void shouldListOnlyPendingPayments() {
        Instant old = Instant.now().minusSeconds(600);
        UUID created = payment("created", "ORD1", old);
        UUID actionRequired = payment("ACTION_REQUIRED", "ORD2", old);
        payment("processed", "ORD3", old);
        payment("failed", "ORD4", old);
        payment("canceled", "ORD5", old);
        payment("refunded", "ORD6", old);
        payment("created", null, old);
        payment("created", "ORD8", Instant.now().plusSeconds(600));

        assertThat(pending(Instant.now())).containsExactlyInAnyOrder(created, actionRequired);
    }

    @Test
    @DisplayName("Deve respeitar o limite do lote e a ordem do mais antigo para o mais novo")
    void shouldRespectLimitAndOrder() {
        UUID oldest = payment("created", "ORD1", Instant.now().minusSeconds(900));
        payment("created", "ORD2", Instant.now().minusSeconds(600));
        payment("created", "ORD3", Instant.now().minusSeconds(300));

        List<UUID> batch = gateway.findPendingForVerification(Instant.now(), 2).stream().map(Payment::id).toList();

        assertThat(batch).hasSize(2).first().isEqualTo(oldest);
    }
}
