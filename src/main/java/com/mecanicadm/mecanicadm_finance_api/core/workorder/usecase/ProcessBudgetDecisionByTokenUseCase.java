package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.BudgetDecisionToken;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.DecideWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.ProcessBudgetDecisionByTokenCommand;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.VoidUseCase;
import org.springframework.transaction.annotation.Transactional;

public class ProcessBudgetDecisionByTokenUseCase implements VoidUseCase<ProcessBudgetDecisionByTokenCommand> {

    private final BudgetDecisionTokenGateway tokenGateway;
    private final DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase;

    public ProcessBudgetDecisionByTokenUseCase(BudgetDecisionTokenGateway tokenGateway,
                                               DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase) {
        this.tokenGateway = tokenGateway;
        this.decideWorkOrderBudgetUseCase = decideWorkOrderBudgetUseCase;
    }

    public void validateToken(String tokenValue) {
        BudgetDecisionToken token = tokenGateway.findByToken(tokenValue)
                .orElseThrow(BudgetExceptions.BudgetTokenNotFound::new);

        if (!token.isValid()) {
            throw BudgetExceptions.BudgetTokenInvalid.of(token.isUsed() ? "já utilizado" : "expirado");
        }
    }

    @Override
    @Transactional
    public void execute(ProcessBudgetDecisionByTokenCommand cmd) {
        BudgetDecisionToken token = tokenGateway.findByToken(cmd.token())
                .orElseThrow(BudgetExceptions.BudgetTokenNotFound::new);

        if (!token.isValid()) {
            throw BudgetExceptions.BudgetTokenInvalid.of(token.isUsed() ? "já utilizado" : "expirado");
        }

        WorkOrderBudgetStatus decision = WorkOrderBudgetStatus.valueOf(cmd.action());
        String observation = cmd.observation();

        decideWorkOrderBudgetUseCase.execute(
                new DecideWorkOrderBudgetCommand(token.getWorkOrderId(), decision, observation));

        token.markAsUsed();
        tokenGateway.update(token);
    }
}
