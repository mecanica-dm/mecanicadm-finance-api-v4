package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailAttachment;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailService;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetVehicle;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.SendWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableBudgetResponse;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.query.GetPrintableBudgetQuery;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.VoidUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;
import java.util.UUID;

public class SendWorkOrderBudgetUseCase implements VoidUseCase<SendWorkOrderBudgetCommand> {

    private static final Logger logger = LoggerFactory.getLogger(SendWorkOrderBudgetUseCase.class);

    private final BudgetSendTransactionService transactionService;
    private final EmailService emailService;
    private final GetPrintableBudgetUseCase getPrintableBudgetUseCase;
    private final String budgetDecisionPath;

    public SendWorkOrderBudgetUseCase(BudgetSendTransactionService transactionService,
                                      EmailService emailService,
                                      GetPrintableBudgetUseCase getPrintableBudgetUseCase,
                                      @Value("${app.budget.decision-path:/budget-decision/}") String budgetDecisionPath) {
        this.transactionService = transactionService;
        this.emailService = emailService;
        this.getPrintableBudgetUseCase = getPrintableBudgetUseCase;
        this.budgetDecisionPath = budgetDecisionPath;
    }

    public void execute(SendWorkOrderBudgetCommand cmd) {
        BudgetSendTransactionService.PreparedSend prepared = transactionService.prepare(cmd.workOrderId());
        WorkOrderBudget budget = prepared.budget();

        String clientEmail = resolveClientEmail(budget.getInfo());
        if (clientEmail == null) {
            return;
        }

        try {
            String subject = "Orçamento - Ordem de Serviço #" + budget.getWorkOrderId();
            String htmlBody = buildEmailHtml(budget, prepared.token().getToken(), cmd.baseUrl());
            EmailAttachment attachment = generatePdfAttachment(budget.getWorkOrderId());
            emailService.send(clientEmail, subject, htmlBody, attachment);
        } catch (RuntimeException e) {
            transactionService.registerFailure(budget.getWorkOrderId());
            throw e;
        }
    }

    private String resolveClientEmail(WorkOrderBudgetInfo info) {
        WorkOrderBudgetClient client = info == null ? null : info.getClient();
        return client == null ? null : client.getEmail();
    }

    private EmailAttachment generatePdfAttachment(UUID workOrderId) {
        try {
            PrintableBudgetResponse response = getPrintableBudgetUseCase.execute(
                    new GetPrintableBudgetQuery(workOrderId));
            byte[] pdfBytes = Base64.getDecoder().decode(response.base64Content());
            return new EmailAttachment(response.fileName(), pdfBytes);
        } catch (Exception e) {
            logger.warn("Não foi possível gerar PDF do orçamento para OS {}: {}", workOrderId, e.getMessage());
            return null;
        }
    }

    private String buildEmailHtml(WorkOrderBudget budget, String token, String baseUrl) {
        String approveUrl = baseUrl + budgetDecisionPath + token + "/form?action=APPROVED";
        String rejectUrl = baseUrl + budgetDecisionPath + token + "/form?action=REJECTED";
        String changesUrl = baseUrl + budgetDecisionPath + token + "/form?action=CHANGES_REQUESTED";

        WorkOrderBudgetInfo info = budget.getInfo();
        WorkOrderBudgetVehicle vehicle = info == null ? null : info.getVehicle();
        String vehicleDescription = vehicle == null ? "" : vehicle.getLicensePlate();
        String description = info == null || info.getDescription() == null ? "" : info.getDescription();

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #333;">Orçamento - Ordem de Serviço #%s</h2>
                    <p>Prezado(a) cliente,</p>
                    <p>Segue abaixo o orçamento para a sua ordem de serviço. O detalhamento completo está no arquivo PDF em anexo.</p>
                    <div style="background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <p><strong>Veículo:</strong> %s</p>
                        <p><strong>Descrição:</strong> %s</p>
                        <p style="font-size: 1.2em; color: #2e7d32;"><strong>Valor Total: R$ %s</strong></p>
                    </div>
                    <p>Por favor, responda ao orçamento clicando em um dos botões abaixo:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: #4caf50; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 5px; display: inline-block;">Aprovar</a>
                        <a href="%s" style="background: #f44336; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 5px; display: inline-block;">Rejeitar</a>
                        <a href="%s" style="background: #ff9800; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 5px; display: inline-block;">Solicitar Alterações</a>
                    </div>
                    <p style="color: #666; font-size: 0.9em;">Este link é válido por 24 horas.</p>
                    <hr style="border: none; border-top: 1px solid #eee;">
                    <p style="color: #999; font-size: 0.8em;">MecânicaDM - Sistema de Gestão de Oficina Mecânica</p>
                </body>
                </html>
                """.formatted(
                budget.getWorkOrderId().toString().substring(0, 8),
                vehicleDescription,
                description,
                budget.getTotalPrice(),
                approveUrl,
                rejectUrl,
                changesUrl
        );
    }
}
