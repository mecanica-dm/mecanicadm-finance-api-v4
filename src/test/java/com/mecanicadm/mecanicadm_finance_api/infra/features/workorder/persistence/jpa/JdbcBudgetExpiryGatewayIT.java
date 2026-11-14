package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway.ExpiredBudget;
import com.mecanicadm.mecanicadm_finance_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcBudgetExpiryGatewayIT extends AbstractIntegrationTest {

    @Autowired
    private BudgetExpiryGateway gateway;
    @Autowired
    private JdbcTemplate jdbc;

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM budget_decision_tokens");
        jdbc.update("DELETE FROM work_order_budgets");
    }

    private UUID budget(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO work_order_budgets (work_order_id, total_price, status, observation) VALUES (?, 100, ?, NULL)", id, status);
        return id;
    }

    private UUID token(UUID workOrderId, boolean used, LocalDateTime createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO budget_decision_tokens (id, work_order_id, token, used, created_at) VALUES (?, ?, ?, ?, ?)",
                id, workOrderId, UUID.randomUUID().toString(), used, createdAt);
        return id;
    }

    private List<ExpiredBudget> claim() {
        return gateway.claimExpired(now().minusHours(24), 50);
    }

    @Test
    @DisplayName("Deve reservar o token vencido e nao usado de um orcamento aguardando decisao")
    void shouldClaimExpiredUnusedToken() {
        UUID workOrder = budget("WAITING_DECISION");
        UUID token = token(workOrder, false, now().minusHours(30));

        assertThat(claim()).containsExactly(new ExpiredBudget(token, workOrder));
    }

    @Test
    @DisplayName("Nao deve reservar token ainda valido, ja usado ou de orcamento que nao aguarda decisao")
    void shouldIgnoreValidUsedOrDecidedBudgets() {
        token(budget("WAITING_DECISION"), false, now().minusHours(2));
        token(budget("WAITING_DECISION"), true, now().minusHours(30));
        token(budget("APPROVED"), false, now().minusHours(30));
        token(budget("CANCELED"), false, now().minusHours(30));

        assertThat(claim()).isEmpty();
    }

    @Test
    @DisplayName("Nao deve reservar o token antigo quando a OS ja tem um token mais recente")
    void shouldIgnoreTokenReplacedByNewerOne() {
        UUID workOrder = budget("WAITING_DECISION");
        token(workOrder, false, now().minusHours(30));
        token(workOrder, false, now().minusHours(2));

        assertThat(claim()).isEmpty();
    }

    @Test
    @DisplayName("Deve avisar uma unica vez: depois de marcado, o token nao e reservado de novo")
    void shouldNotifyOnlyOnce() {
        UUID workOrder = budget("WAITING_DECISION");
        UUID token = token(workOrder, false, now().minusHours(30));

        assertThat(claim()).hasSize(1);
        gateway.markExpiryNotified(token);

        assertThat(claim()).isEmpty();
    }

    @Test
    @DisplayName("Deve respeitar o limite do lote")
    void shouldRespectBatchLimit() {
        for (int i = 0; i < 3; i++) {
            token(budget("WAITING_DECISION"), false, now().minusHours(30 + i));
        }

        assertThat(gateway.claimExpired(now().minusHours(24), 2)).hasSize(2);
    }
}
