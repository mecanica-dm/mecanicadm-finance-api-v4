package com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.InboxGateway;
import com.mecanicadm.mecanicadm_finance_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcInboxGatewayIT extends AbstractIntegrationTest {

    @Autowired
    private InboxGateway inboxGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanInbox() {
        jdbcTemplate.update("DELETE FROM inbox_messages");
    }

    @Test
    @DisplayName("Deve reservar, impedir duplicidade e marcar como processada")
    void shouldStartAndMarkProcessed() {
        InboxEntry entry = entry("budget");

        assertThat(inboxGateway.tryStart(entry)).isTrue();
        assertThat(inboxGateway.tryStart(entry)).isFalse();

        inboxGateway.markProcessed(entry);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM inbox_messages WHERE message_id = ? AND consumer = ?",
                String.class,
                entry.messageId(),
                entry.consumer());
        assertThat(status).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Deve permitir o mesmo messageId para consumidores diferentes")
    void shouldAllowSameMessageForDifferentConsumers() {
        UUID messageId = UUID.randomUUID();

        assertThat(inboxGateway.tryStart(entry(messageId, "budget"))).isTrue();
        assertThat(inboxGateway.tryStart(entry(messageId, "another-queue"))).isTrue();
    }

    @Test
    @DisplayName("Deve remover apenas mensagens PROCESSED antigas")
    void shouldDeleteOnlyOldProcessedMessages() {
        InboxEntry oldProcessed = entry("budget");
        inboxGateway.tryStart(oldProcessed);
        inboxGateway.markProcessed(oldProcessed);
        jdbcTemplate.update(
                "UPDATE inbox_messages SET processed_at = now() - interval '40 days' WHERE message_id = ?",
                oldProcessed.messageId());

        InboxEntry recentProcessed = entry("budget");
        inboxGateway.tryStart(recentProcessed);
        inboxGateway.markProcessed(recentProcessed);

        InboxEntry processing = entry("budget");
        inboxGateway.tryStart(processing);
        jdbcTemplate.update(
                "UPDATE inbox_messages SET received_at = now() - interval '40 days' WHERE message_id = ?",
                processing.messageId());

        int deleted = inboxGateway.deleteProcessedBefore(Instant.now().minus(Duration.ofDays(30)));

        assertThat(deleted).isEqualTo(1);
        assertThat(count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve permitir apenas uma reserva em entregas concorrentes")
    void shouldAllowOnlyOneConcurrentStart() throws Exception {
        InboxEntry entry = entry("budget");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> tryStartConcurrently(entry, ready, start));
            Future<Boolean> second = executor.submit(() -> tryStartConcurrently(entry, ready, start));

            ready.await();
            start.countDown();

            List<Boolean> results = List.of(first.get(), second.get());

            assertThat(results).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean tryStartConcurrently(InboxEntry entry, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            return inboxGateway.tryStart(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inbox_messages", Long.class);
        return count == null ? 0 : count;
    }

    private InboxEntry entry(String consumer) {
        return entry(UUID.randomUUID(), consumer);
    }

    private InboxEntry entry(UUID messageId, String consumer) {
        return new InboxEntry(messageId, consumer, "BudgetGeneratedEvent", UUID.randomUUID(), Instant.now());
    }
}
