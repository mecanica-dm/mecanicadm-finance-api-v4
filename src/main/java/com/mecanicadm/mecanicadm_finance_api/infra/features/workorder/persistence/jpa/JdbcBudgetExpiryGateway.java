package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcBudgetExpiryGateway implements BudgetExpiryGateway {

    private static final String CLAIM_SQL = """
            SELECT t.id AS token_id, t.work_order_id
            FROM budget_decision_tokens t
            JOIN work_order_budgets b ON b.work_order_id = t.work_order_id
            WHERE t.used = FALSE
              AND t.expiry_notified = FALSE
              AND t.created_at < :createdBefore
              AND b.status = 'WAITING_DECISION'
              AND NOT EXISTS (SELECT 1 FROM budget_decision_tokens newer
                              WHERE newer.work_order_id = t.work_order_id AND newer.created_at > t.created_at)
            ORDER BY t.created_at
            LIMIT :limit
            FOR UPDATE OF t SKIP LOCKED
            """;

    private static final String MARK_SQL = """
            UPDATE budget_decision_tokens SET expiry_notified = TRUE WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcBudgetExpiryGateway(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExpiredBudget> claimExpired(LocalDateTime createdBefore, int limit) {
        return jdbcTemplate.query(CLAIM_SQL,
                new MapSqlParameterSource().addValue("createdBefore", createdBefore).addValue("limit", limit),
                (rs, rowNum) -> new ExpiredBudget(rs.getObject("token_id", UUID.class), rs.getObject("work_order_id", UUID.class)));
    }

    @Override
    public void markExpiryNotified(UUID tokenId) {
        jdbcTemplate.update(MARK_SQL, new MapSqlParameterSource("id", tokenId));
    }
}
