package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.schedule;

import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.VerifyPendingPaymentsUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.VerifyPendingPaymentsCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config.PaymentVerificationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerifyPendingPaymentsJobTest {

    private final VerifyPendingPaymentsUseCase useCase = mock(VerifyPendingPaymentsUseCase.class);
    private final PaymentVerificationProperties properties = new PaymentVerificationProperties();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final VerifyPendingPaymentsJob job = new VerifyPendingPaymentsJob(useCase, properties, registry);

    @Test
    @DisplayName("Deve repassar a idade minima e o lote configurados e contar os pagamentos atualizados")
    void shouldUseConfigurationAndCountUpdates() {
        properties.setMinAgeSeconds(120);
        properties.setBatchSize(5);
        when(useCase.execute(new VerifyPendingPaymentsCommand(Duration.ofSeconds(120), 5))).thenReturn(2);

        job.verifyPendingPayments();

        assertThat(registry.counter("payment.verification.updated").count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Nao deve propagar falha da rotina, para o agendador continuar")
    void shouldSwallowFailures() {
        when(useCase.execute(any())).thenThrow(new IllegalStateException("banco fora"));

        assertThatCode(job::verifyPendingPayments).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Propriedades devem ter valores padrao e aceitar alteracao")
    void propertiesShouldHaveDefaultsAndSetters() {
        PaymentVerificationProperties p = new PaymentVerificationProperties();
        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getIntervalMs()).isEqualTo(60000);
        assertThat(p.getInitialDelayMs()).isEqualTo(60000);
        assertThat(p.getMinAgeSeconds()).isEqualTo(60);
        assertThat(p.getBatchSize()).isEqualTo(20);
        p.setEnabled(false);
        p.setIntervalMs(1);
        p.setInitialDelayMs(2);
        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getIntervalMs()).isEqualTo(1);
        assertThat(p.getInitialDelayMs()).isEqualTo(2);
    }
}
