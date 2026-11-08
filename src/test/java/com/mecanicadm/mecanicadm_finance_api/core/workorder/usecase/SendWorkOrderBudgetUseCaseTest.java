package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailAttachment;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailService;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.BudgetDecisionToken;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetVehicle;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command.SendWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableBudgetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendWorkOrderBudgetUseCaseTest {

    @Mock
    private BudgetSendTransactionService transactionService;
    @Mock
    private EmailService emailService;
    @Mock
    private GetPrintableBudgetUseCase getPrintableBudgetUseCase;

    private SendWorkOrderBudgetUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendWorkOrderBudgetUseCase(
                transactionService, emailService, getPrintableBudgetUseCase, "/budget-decision/");
    }

    @Test
    @DisplayName("Deve preparar o envio transacional e enviar orçamento por e-mail")
    void shouldSendBudgetByEmail() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, "cliente@teste.com");
        when(transactionService.prepare(workOrderId))
                .thenReturn(new BudgetSendTransactionService.PreparedSend(
                        budget, BudgetDecisionToken.create(workOrderId)));
        when(getPrintableBudgetUseCase.execute(any())).thenReturn(
                new PrintableBudgetResponse("orcamento.pdf",
                        Base64.getEncoder().encodeToString("pdf".getBytes())));

        useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api"));

        verify(transactionService).prepare(workOrderId);
        verify(emailService).send(eq("cliente@teste.com"), anyString(), anyString(), any(EmailAttachment.class));
    }

    @Test
    @DisplayName("Deve pular o e-mail quando o cliente não tiver e-mail")
    void shouldSkipEmailWhenClientEmailMissing() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, null);
        when(transactionService.prepare(workOrderId))
                .thenReturn(new BudgetSendTransactionService.PreparedSend(
                        budget, BudgetDecisionToken.create(workOrderId)));

        useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api"));

        verify(transactionService).prepare(workOrderId);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve registrar a falha em nova transação quando o envio de e-mail falhar")
    void shouldRegisterFailureWhenEmailFails() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, "cliente@teste.com");
        when(transactionService.prepare(workOrderId))
                .thenReturn(new BudgetSendTransactionService.PreparedSend(
                        budget, BudgetDecisionToken.create(workOrderId)));
        when(getPrintableBudgetUseCase.execute(any())).thenReturn(
                new PrintableBudgetResponse("orcamento.pdf",
                        Base64.getEncoder().encodeToString("pdf".getBytes())));
        doThrow(new RuntimeException("smtp down"))
                .when(emailService).send(anyString(), anyString(), anyString(), any());

        assertThrows(RuntimeException.class,
                () -> useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api")));

        verify(transactionService).registerFailure(workOrderId);
    }

    @Test
    @DisplayName("Deve propagar exceção quando o orçamento não for encontrado")
    void shouldThrowWhenBudgetNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(transactionService.prepare(workOrderId))
                .thenThrow(new BudgetExceptions.BudgetNotFound());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api")));

        verify(transactionService, never()).registerFailure(any());
        verifyNoInteractions(emailService);
    }

    private WorkOrderBudget budget(UUID workOrderId, String clientEmail) {
        WorkOrderBudgetClient client = WorkOrderBudgetClient.create(
                "Cliente", clientEmail, "12345678901", "48999999999");
        WorkOrderBudgetInfo info = new WorkOrderBudgetInfo(
                client,
                WorkOrderBudgetVehicle.create("ABC-1234", "Modelo", "Marca", (short) 2020),
                "descricao",
                null);
        return WorkOrderBudget.restore(
                workOrderId, new BigDecimal("180.00"), WorkOrderBudgetStatus.PENDING, null, info);
    }
}
