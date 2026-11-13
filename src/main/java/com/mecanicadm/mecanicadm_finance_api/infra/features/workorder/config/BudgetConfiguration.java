package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.config;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailService;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.BudgetSendTransactionService;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CalculateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CancelWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.DecideWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.GetPrintableBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.ManuallyAdjustWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.ProcessBudgetDecisionByTokenUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.SendWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.pdf.PdfGenerator;
import org.springframework.beans.factory.annotation.Value;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetExpiryGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.ExpireWorkOrderBudgetsUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BudgetExpirationProperties.class)
public class BudgetConfiguration {

    @Bean
    public ExpireWorkOrderBudgetsUseCase expireWorkOrderBudgetsUseCase(BudgetExpiryGateway budgetExpiryGateway,
                                                                       EventPublisher eventPublisher) {
        return new ExpireWorkOrderBudgetsUseCase(budgetExpiryGateway, eventPublisher);
    }

    @Bean
    public BudgetSendTransactionService budgetSendTransactionService(WorkOrderBudgetGateway budgetGateway,
                                                                     BudgetDecisionTokenGateway budgetDecisionTokenGateway,
                                                                     EventPublisher eventPublisher) {
        return new BudgetSendTransactionService(budgetGateway, budgetDecisionTokenGateway, eventPublisher);
    }

    @Bean
    public SendWorkOrderBudgetUseCase sendWorkOrderBudgetUseCase(BudgetSendTransactionService transactionService,
                                                                 EmailService emailService,
                                                                 GetPrintableBudgetUseCase getPrintableBudgetUseCase,
                                                                 @Value("${app.budget.decision-path:/budget-decision/}") String budgetDecisionPath) {
        return new SendWorkOrderBudgetUseCase(transactionService, emailService,
                getPrintableBudgetUseCase, budgetDecisionPath);
    }

    @Bean
    public GetPrintableBudgetUseCase getPrintableBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                               PdfGenerator pdfGenerator) {
        return new GetPrintableBudgetUseCase(budgetGateway, pdfGenerator);
    }

    @Bean
    public CalculateWorkOrderBudgetUseCase calculateWorkOrderBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                                           EventPublisher eventPublisher) {
        return new CalculateWorkOrderBudgetUseCase(budgetGateway, eventPublisher);
    }

    @Bean
    public ManuallyAdjustWorkOrderBudgetUseCase manuallyAdjustWorkOrderBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                                                     EventPublisher eventPublisher) {
        return new ManuallyAdjustWorkOrderBudgetUseCase(budgetGateway, eventPublisher);
    }

    @Bean
    public DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                                     EventPublisher eventPublisher) {
        return new DecideWorkOrderBudgetUseCase(budgetGateway, eventPublisher);
    }

    @Bean
    public CancelWorkOrderBudgetUseCase cancelWorkOrderBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                                     EventPublisher eventPublisher) {
        return new CancelWorkOrderBudgetUseCase(budgetGateway, eventPublisher);
    }

    @Bean
    public CreateWorkOrderBudgetUseCase createWorkOrderBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                                     EventPublisher eventPublisher) {
        return new CreateWorkOrderBudgetUseCase(budgetGateway, eventPublisher);
    }

    @Bean
    public ProcessBudgetDecisionByTokenUseCase processBudgetDecisionByTokenUseCase(
            BudgetDecisionTokenGateway tokenGateway,
            DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase) {
        return new ProcessBudgetDecisionByTokenUseCase(tokenGateway, decideWorkOrderBudgetUseCase);
    }
}
