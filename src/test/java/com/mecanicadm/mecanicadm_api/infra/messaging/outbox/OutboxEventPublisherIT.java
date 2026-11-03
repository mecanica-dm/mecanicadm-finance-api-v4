package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetApprovedEvent;
import com.mecanicadm.mecanicadm_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OutboxEventPublisherIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate.update("DELETE FROM outbox_messages");
    }

    @Test
    @DisplayName("Deve persistir o evento quando a transação de negócio é confirmada")
    void shouldPersistEventWhenTransactionCommits() {
        UUID workOrderId = UUID.randomUUID();
        BudgetApprovedEvent event = new BudgetApprovedEvent(workOrderId);

        transactionTemplate.executeWithoutResult(status -> publisher.publish(event));

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_messages WHERE id = ?", Long.class, event.messageId());
        assertThat(count).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT event_type FROM outbox_messages WHERE id = ?", String.class, event.messageId()))
                .isEqualTo(BudgetApprovedEvent.class.getSimpleName());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT aggregate_id FROM outbox_messages WHERE id = ?", UUID.class, event.messageId()))
                .isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Deve descartar o evento quando a transação de negócio é revertida")
    void shouldDiscardEventWhenTransactionRollsBack() {
        BudgetApprovedEvent event = new BudgetApprovedEvent(UUID.randomUUID());

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(event);
            throw new IllegalStateException("falha de negócio");
        })).isInstanceOf(IllegalStateException.class);

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_messages", Long.class);
        assertThat(count).isZero();
    }
}
