package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.config;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailService;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.BudgetSendTransactionService;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.DecideWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.GetPrintableBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.pdf.PdfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class BudgetConfigurationTest {

    @Mock
    private WorkOrderBudgetGateway budgetGateway;
    @Mock
    private BudgetDecisionTokenGateway tokenGateway;
    @Mock
    private EmailService emailService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private PdfGenerator pdfGenerator;
    @Mock
    private DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase;
    @Mock
    private BudgetSendTransactionService budgetSendTransactionService;

    private BudgetConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new BudgetConfiguration();
    }

    @Test
    @DisplayName("Deve criar bean CalculateWorkOrderBudgetUseCase")
    void shouldCreateCalculateWorkOrderBudgetUseCase() {
        assertNotNull(configuration.calculateWorkOrderBudgetUseCase(budgetGateway, eventPublisher));
    }

    @Test
    @DisplayName("Deve criar bean CreateWorkOrderBudgetUseCase")
    void shouldCreateCreateWorkOrderBudgetUseCase() {
        assertNotNull(configuration.createWorkOrderBudgetUseCase(budgetGateway, eventPublisher));
    }

    @Test
    @DisplayName("Deve criar bean ManuallyAdjustWorkOrderBudgetUseCase")
    void shouldCreateManuallyAdjustWorkOrderBudgetUseCase() {
        assertNotNull(configuration.manuallyAdjustWorkOrderBudgetUseCase(budgetGateway, eventPublisher));
    }

    @Test
    @DisplayName("Deve criar bean DecideWorkOrderBudgetUseCase")
    void shouldCreateDecideWorkOrderBudgetUseCase() {
        assertNotNull(configuration.decideWorkOrderBudgetUseCase(budgetGateway, eventPublisher));
    }

    @Test
    @DisplayName("Deve criar bean CancelWorkOrderBudgetUseCase")
    void shouldCreateCancelWorkOrderBudgetUseCase() {
        assertNotNull(configuration.cancelWorkOrderBudgetUseCase(budgetGateway, eventPublisher));
    }

    @Test
    @DisplayName("Deve criar bean GetPrintableBudgetUseCase")
    void shouldCreateGetPrintableBudgetUseCase() {
        assertNotNull(configuration.getPrintableBudgetUseCase(budgetGateway, pdfGenerator));
    }

    @Test
    @DisplayName("Deve criar bean BudgetSendTransactionService")
    void shouldCreateBudgetSendTransactionService() {
        assertNotNull(configuration.budgetSendTransactionService(
                budgetGateway, tokenGateway, eventPublisher));
    }

    @Test
    @DisplayName("Deve criar bean SendWorkOrderBudgetUseCase")
    void shouldCreateSendWorkOrderBudgetUseCase() {
        GetPrintableBudgetUseCase printable = configuration.getPrintableBudgetUseCase(budgetGateway, pdfGenerator);

        assertNotNull(configuration.sendWorkOrderBudgetUseCase(
                budgetSendTransactionService, emailService, printable));
    }

    @Test
    @DisplayName("Deve criar bean ProcessBudgetDecisionByTokenUseCase")
    void shouldCreateProcessBudgetDecisionByTokenUseCase() {
        assertNotNull(configuration.processBudgetDecisionByTokenUseCase(tokenGateway, decideWorkOrderBudgetUseCase));
    }
}
