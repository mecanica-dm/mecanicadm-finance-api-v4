package com.mecanicadm.mecanicadm_api.infra.features.workorder.config;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EmailService;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.CalculateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.CancelWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.CreateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.DecideWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.GetPrintableBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.ManuallyAdjustWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.ProcessBudgetDecisionByTokenUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.SendWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.infra.pdf.PdfGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BudgetConfiguration {

    @Bean
    public SendWorkOrderBudgetUseCase sendWorkOrderBudgetUseCase(WorkOrderBudgetGateway budgetGateway,
                                                                 BudgetDecisionTokenGateway budgetDecisionTokenGateway,
                                                                 EmailService emailService,
                                                                 GetPrintableBudgetUseCase getPrintableBudgetUseCase,
                                                                 EventPublisher eventPublisher,
                                                                 @Value("${app.budget.decision-path:/budget-decision/}") String budgetDecisionPath) {
        return new SendWorkOrderBudgetUseCase(budgetGateway, budgetDecisionTokenGateway, emailService,
                getPrintableBudgetUseCase, eventPublisher, budgetDecisionPath);
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
