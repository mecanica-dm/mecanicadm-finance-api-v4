package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.schedule;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.ExpireWorkOrderBudgetsUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.config.BudgetExpirationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetExpirationJobTest {

    private final ExpireWorkOrderBudgetsUseCase useCase = mock(ExpireWorkOrderBudgetsUseCase.class);
    private final BudgetExpirationProperties properties = new BudgetExpirationProperties();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BudgetExpirationJob job = new BudgetExpirationJob(useCase, properties, registry);

    @Test
    @DisplayName("Deve contar os orcamentos avisados usando o tamanho de lote configurado")
    void shouldCountNotifiedBudgets() {
        properties.setBatchSize(10);
        when(useCase.execute(10)).thenReturn(3);

        job.expireBudgets();

        assertThat(registry.counter("budget.expired.notified").count()).isEqualTo(3);
    }

    @Test
    @DisplayName("Nao deve propagar falha da rotina, para o agendador continuar")
    void shouldSwallowFailures() {
        when(useCase.execute(50)).thenThrow(new IllegalStateException("banco fora"));

        assertThatCode(job::expireBudgets).doesNotThrowAnyException();
        assertThat(registry.counter("budget.expired.notified").count()).isZero();
    }

    @Test
    @DisplayName("Propriedades devem ter valores padrao e aceitar alteracao")
    void propertiesShouldHaveDefaultsAndSetters() {
        BudgetExpirationProperties p = new BudgetExpirationProperties();
        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getIntervalMs()).isEqualTo(60000);
        assertThat(p.getInitialDelayMs()).isEqualTo(30000);
        p.setEnabled(false);
        p.setIntervalMs(5);
        p.setInitialDelayMs(1);
        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getIntervalMs()).isEqualTo(5);
        assertThat(p.getInitialDelayMs()).isEqualTo(1);
    }
}
