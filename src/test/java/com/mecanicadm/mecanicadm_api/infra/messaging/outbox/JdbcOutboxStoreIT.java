package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import com.mecanicadm.mecanicadm_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcOutboxStoreIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxStore outboxStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.update("DELETE FROM outbox_messages");
    }

    @Test
    @DisplayName("Deve gravar, reivindicar e marcar mensagem como publicada")
    void shouldSaveClaimAndPublish() {
        OutboxMessage message = message();
        outboxStore.save(message);

        List<OutboxMessage> claimed = outboxStore.claim(10);

        assertThat(claimed).hasSize(1);
        OutboxMessage claimedMessage = claimed.getFirst();
        assertThat(claimedMessage.id()).isEqualTo(message.id());
        assertThat(claimedMessage.eventType()).isEqualTo(message.eventType());
        assertThat(claimedMessage.aggregateId()).isEqualTo(message.aggregateId());
        assertThat(claimedMessage.payload()).isEqualToIgnoringWhitespace(message.payload());
        assertThat(outboxStore.countByStatus(OutboxStatus.PENDING)).isZero();
        assertThat(outboxStore.countByStatus(OutboxStatus.PROCESSING)).isEqualTo(1);

        outboxStore.markPublished(message.id());

        assertThat(outboxStore.countByStatus(OutboxStatus.PROCESSING)).isZero();
        assertThat(outboxStore.countByStatus(OutboxStatus.PUBLISHED)).isEqualTo(1);
        OffsetDateTime publishedAt = jdbcTemplate.queryForObject(
                "SELECT published_at FROM outbox_messages WHERE id = ?",
                OffsetDateTime.class,
                message.id());
        assertThat(publishedAt).isNotNull();
    }

    @Test
    @DisplayName("Deve ignorar gravação duplicada do mesmo messageId")
    void shouldIgnoreDuplicateMessageId() {
        OutboxMessage message = message();

        outboxStore.save(message);
        outboxStore.save(message);

        assertThat(outboxStore.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve marcar PROCESSING expirado como FAILED")
    void shouldMoveStaleProcessingToFailed() {
        OutboxMessage message = message();
        outboxStore.save(message);
        outboxStore.claim(10);
        jdbcTemplate.update(
                "UPDATE outbox_messages SET updated_at = now() - interval '2 minutes' WHERE id = ?",
                message.id());

        int moved = outboxStore.moveStaleProcessingToFailed(Duration.ofSeconds(60));

        assertThat(moved).isEqualTo(1);
        assertThat(outboxStore.countByStatus(OutboxStatus.FAILED)).isEqualTo(1);
        assertThat(outboxStore.countByStatus(OutboxStatus.PROCESSING)).isZero();
    }

    @Test
    @DisplayName("Deve remover apenas mensagens PUBLISHED antigas, preservando FAILED")
    void shouldDeleteOnlyOldPublishedMessages() {
        OutboxMessage published = message();
        outboxStore.save(published);
        outboxStore.claim(10);
        outboxStore.markPublished(published.id());
        jdbcTemplate.update(
                "UPDATE outbox_messages SET published_at = now() - interval '40 days' WHERE id = ?",
                published.id());

        OutboxMessage failed = message();
        outboxStore.save(failed);
        outboxStore.claim(10);
        outboxStore.markFailed(failed.id(), "falha");
        jdbcTemplate.update(
                "UPDATE outbox_messages SET updated_at = now() - interval '40 days' WHERE id = ?",
                failed.id());

        int deleted = outboxStore.deletePublishedBefore(Instant.now().minus(Duration.ofDays(30)));

        assertThat(deleted).isEqualTo(1);
        assertThat(outboxStore.countByStatus(OutboxStatus.PUBLISHED)).isZero();
        assertThat(outboxStore.countByStatus(OutboxStatus.FAILED)).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve calcular a idade da mensagem pendente mais antiga")
    void shouldCalculateOldestPendingAge() {
        outboxStore.save(message());
        jdbcTemplate.update(
                "UPDATE outbox_messages SET created_at = now() - interval '10 seconds'");

        assertThat(outboxStore.oldestPendingAgeSeconds()).isGreaterThanOrEqualTo(10);
    }

    private OutboxMessage message() {
        return new OutboxMessage(
                UUID.randomUUID(),
                "BudgetApprovedEvent",
                UUID.randomUUID(),
                "{\"workOrderId\":\"" + UUID.randomUUID() + "\"}",
                Instant.now());
    }
}
