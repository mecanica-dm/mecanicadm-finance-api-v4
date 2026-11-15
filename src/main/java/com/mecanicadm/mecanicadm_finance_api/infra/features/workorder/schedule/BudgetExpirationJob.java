package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.schedule;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.ExpireWorkOrderBudgetsUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.config.BudgetExpirationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.budget.expiration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BudgetExpirationJob {

    private static final Logger logger = LoggerFactory.getLogger(BudgetExpirationJob.class);

    private final ExpireWorkOrderBudgetsUseCase useCase;
    private final BudgetExpirationProperties properties;
    private final MeterRegistry meterRegistry;

    public BudgetExpirationJob(ExpireWorkOrderBudgetsUseCase useCase,
                               BudgetExpirationProperties properties,
                               MeterRegistry meterRegistry) {
        this.useCase = useCase;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(initialDelayString = "${app.budget.expiration.initial-delay-ms:30000}",
            fixedDelayString = "${app.budget.expiration.interval-ms:60000}")
    public void expireBudgets() {
        try {
            int expired = useCase.execute(properties.getBatchSize());
            if (expired > 0) {
                meterRegistry.counter("budget.expired.notified").increment(expired);
                logger.info("Avisada a expiracao de {} orcamento(s) sem decisao do cliente", expired);
            }
        } catch (RuntimeException e) {
            logger.error("Erro ao verificar a expiracao dos orcamentos", e);
        }
    }
}
