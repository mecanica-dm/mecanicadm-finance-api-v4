package com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.InboxGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
@ConditionalOnProperty(prefix = "app.inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcInboxGateway implements InboxGateway {

    private static final String TRY_START_SQL = """
            INSERT INTO inbox_messages
                (message_id, consumer, event_type, aggregate_id, status, received_at, created_at, updated_at)
            VALUES
                (:messageId, :consumer, :eventType, :aggregateId, 'PROCESSING', :receivedAt, now(), now())
            ON CONFLICT (message_id, consumer) DO NOTHING
            """;

    private static final String MARK_PROCESSED_SQL = """
            UPDATE inbox_messages
            SET status = 'PROCESSED',
                processed_at = now(),
                updated_at = now()
            WHERE message_id = :messageId
              AND consumer = :consumer
            """;

    private static final String DELETE_PROCESSED_SQL = """
            DELETE FROM inbox_messages
            WHERE status = 'PROCESSED'
              AND processed_at < :cutoff
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcInboxGateway(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryStart(InboxEntry entry) {
        return jdbcTemplate.update(TRY_START_SQL, params(entry)) == 1;
    }

    @Override
    public void markProcessed(InboxEntry entry) {
        jdbcTemplate.update(MARK_PROCESSED_SQL, params(entry));
    }

    @Override
    public int deleteProcessedBefore(Instant cutoff) {
        return jdbcTemplate.update(DELETE_PROCESSED_SQL,
                new MapSqlParameterSource("cutoff", OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC)));
    }

    private MapSqlParameterSource params(InboxEntry entry) {
        return new MapSqlParameterSource()
                .addValue("messageId", entry.messageId())
                .addValue("consumer", entry.consumer())
                .addValue("eventType", entry.eventType())
                .addValue("aggregateId", entry.aggregateId())
                .addValue("receivedAt", OffsetDateTime.ofInstant(entry.receivedAt(), ZoneOffset.UTC));
    }
}
