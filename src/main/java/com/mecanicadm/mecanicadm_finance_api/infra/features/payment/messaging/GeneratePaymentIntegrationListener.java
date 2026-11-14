package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.GeneratePaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox.InboundMessageContext;
import com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeneratePaymentIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(GeneratePaymentIntegrationListener.class);

    private final CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase;
    private final WorkOrderBudgetGateway budgetGateway;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public GeneratePaymentIntegrationListener(CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase,
                                              WorkOrderBudgetGateway budgetGateway,
                                              RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.createMercadoPagoOrderUseCase = createMercadoPagoOrderUseCase;
        this.budgetGateway = budgetGateway;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(GeneratePaymentIntegrationCommand command) {
        logger.info("Recebido GeneratePaymentIntegrationCommand para a OS {}", command.workOrderId());

        try {
            WorkOrderBudget budget = budgetGateway.findById(command.workOrderId())
                    .orElseThrow(BudgetExceptions.BudgetNotFound::new);
            createMercadoPagoOrderUseCase.execute(toCommand(command, budget));
        } catch (RuntimeException e) {
            if (e instanceof DomainExceptionCore) {
                rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                        command.workOrderId(),
                        GeneratePaymentIntegrationCommand.class.getSimpleName(),
                        e,
                        InboundMessageContext.current()));
            }
            throw e;
        }
    }

    private CreateMercadoPagoOrderCommand toCommand(GeneratePaymentIntegrationCommand command, WorkOrderBudget budget) {
        String workOrderId = command.workOrderId().toString();
        WorkOrderBudgetClient client = budget.getInfo() == null ? null : budget.getInfo().getClient();
        String[] name = splitName(client == null ? null : client.getName());
        return new CreateMercadoPagoOrderCommand(
                workOrderId,
                workOrderId,
                budget.getDescription(),
                budget.getTotalPrice(),
                client == null ? null : client.getEmail(),
                name[0],
                name[1],
                null,
                null,
                null,
                null
        );
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{null, null};
        }
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? new String[]{trimmed, null}
                : new String[]{trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }
}
