package com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {

    @Bean
    public MeterBinder outboxMetrics(OutboxStore outboxStore) {
        return registry -> {
            Gauge.builder("outbox.messages.pending",
                            outboxStore,
                            store -> store.countByStatus(OutboxStatus.PENDING))
                    .description("Mensagens pendentes na outbox")
                    .register(registry);

            Gauge.builder("outbox.messages.failed.current",
                            outboxStore,
                            store -> store.countByStatus(OutboxStatus.FAILED))
                    .description("Mensagens com status FAILED na outbox")
                    .register(registry);

            Gauge.builder("outbox.oldest.pending.age.seconds",
                            outboxStore,
                            OutboxStore::oldestPendingAgeSeconds)
                    .description("Idade da mensagem mais antiga pendente na outbox, em segundos")
                    .register(registry);
        };
    }
}
