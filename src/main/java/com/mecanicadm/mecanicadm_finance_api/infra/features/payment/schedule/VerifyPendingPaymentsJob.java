package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.schedule;

import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.VerifyPendingPaymentsUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.VerifyPendingPaymentsCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config.PaymentVerificationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "app.payment.verification", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VerifyPendingPaymentsJob {

    private static final Logger logger = LoggerFactory.getLogger(VerifyPendingPaymentsJob.class);

    private final VerifyPendingPaymentsUseCase useCase;
    private final PaymentVerificationProperties properties;
    private final MeterRegistry meterRegistry;

    public VerifyPendingPaymentsJob(VerifyPendingPaymentsUseCase useCase,
                                    PaymentVerificationProperties properties,
                                    MeterRegistry meterRegistry) {
        this.useCase = useCase;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(initialDelayString = "${app.payment.verification.initial-delay-ms:60000}",
            fixedDelayString = "${app.payment.verification.interval-ms:60000}")
    public void verifyPendingPayments() {
        try {
            int changed = useCase.execute(new VerifyPendingPaymentsCommand(
                    Duration.ofSeconds(properties.getMinAgeSeconds()), properties.getBatchSize()));
            if (changed > 0) {
                meterRegistry.counter("payment.verification.updated").increment(changed);
                logger.info("Atualizado o resultado de {} pagamento(s) pendente(s)", changed);
            }
        } catch (RuntimeException e) {
            logger.error("Erro ao verificar os pagamentos pendentes", e);
        }
    }
}
