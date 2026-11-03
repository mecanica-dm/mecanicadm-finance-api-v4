package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcOutboxStore implements OutboxStore {

    private static final String INSERT_SQL = """
            INSERT INTO outbox_messages
                (id, event_type, aggregate_id, payload, occurred_at, status, created_at, updated_at)
            VALUES
                (:id, :eventType, :aggregateId, CAST(:payload AS jsonb), :occurredAt, 'PENDING', now(), now())
            ON CONFLICT (id) DO NOTHING
            """;

    private static final String CLAIM_SQL = """
            WITH claimed AS (
                SELECT id
                FROM outbox_messages
                WHERE status = 'PENDING'
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT :batchSize
            )
            UPDATE outbox_messages m
            SET status = 'PROCESSING',
                updated_at = now()
            FROM claimed c
            WHERE m.id = c.id
            RETURNING m.id, m.event_type, m.aggregate_id, m.payload, m.occurred_at
            """;

    private static final String MARK_PUBLISHED_SQL = """
            UPDATE outbox_messages
            SET status = 'PUBLISHED',
                published_at = now(),
                last_error = NULL,
                updated_at = now()
            WHERE id = :id
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE outbox_messages
            SET status = 'FAILED',
                last_error = :error,
                updated_at = now()
            WHERE id = :id
            """;

    private static final String MOVE_STALE_TO_FAILED_SQL = """
            UPDATE outbox_messages
            SET status = 'FAILED',
                last_error = 'Processing timeout: relay não concluiu a publicação',
                updated_at = now()
            WHERE status = 'PROCESSING'
              AND updated_at < now() - (:seconds * interval '1 second')
            """;

    private static final String DELETE_PUBLISHED_SQL = """
            DELETE FROM outbox_messages
            WHERE status = 'PUBLISHED'
              AND published_at < :cutoff
            """;

    private static final String COUNT_BY_STATUS_SQL = """
            SELECT COUNT(*)
            FROM outbox_messages
            WHERE status = :status
            """;

    private static final String OLDEST_PENDING_AGE_SQL = """
            SELECT COALESCE(CAST(EXTRACT(EPOCH FROM now() - MIN(created_at)) AS BIGINT), 0)
            FROM outbox_messages
            WHERE status = 'PENDING'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOutboxStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OutboxMessage message) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", message.id())
                .addValue("eventType", message.eventType())
                .addValue("aggregateId", message.aggregateId())
                .addValue("payload", message.payload())
                .addValue("occurredAt", OffsetDateTime.ofInstant(message.occurredAt(), ZoneOffset.UTC));

        jdbcTemplate.update(INSERT_SQL, params);
    }

    @Override
    public List<OutboxMessage> claim(int batchSize) {
        MapSqlParameterSource params = new MapSqlParameterSource("batchSize", batchSize);
        return jdbcTemplate.query(CLAIM_SQL, params, this::map);
    }

    @Override
    public void markPublished(UUID id) {
        jdbcTemplate.update(MARK_PUBLISHED_SQL, new MapSqlParameterSource("id", id));
    }

    @Override
    public void markFailed(UUID id, String error) {
        jdbcTemplate.update(MARK_FAILED_SQL, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("error", error));
    }

    @Override
    public int moveStaleProcessingToFailed(Duration timeout) {
        return jdbcTemplate.update(MOVE_STALE_TO_FAILED_SQL,
                new MapSqlParameterSource("seconds", timeout.toSeconds()));
    }

    @Override
    public int deletePublishedBefore(Instant cutoff) {
        return jdbcTemplate.update(DELETE_PUBLISHED_SQL,
                new MapSqlParameterSource("cutoff", OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC)));
    }

    @Override
    public long countByStatus(OutboxStatus status) {
        Long count = jdbcTemplate.queryForObject(
                COUNT_BY_STATUS_SQL,
                new MapSqlParameterSource("status", status.name()),
                Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long oldestPendingAgeSeconds() {
        Long age = jdbcTemplate.queryForObject(OLDEST_PENDING_AGE_SQL, new MapSqlParameterSource(), Long.class);
        return age == null ? 0 : age;
    }

    private OutboxMessage map(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime occurredAt = rs.getObject("occurred_at", OffsetDateTime.class);
        return new OutboxMessage(
                rs.getObject("id", UUID.class),
                rs.getString("event_type"),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("payload"),
                occurredAt == null ? null : occurredAt.toInstant()
        );
    }
}
