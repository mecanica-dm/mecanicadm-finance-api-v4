package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EmailAttachment;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EmailService;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.BudgetDecisionToken;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudgetVehicle;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetSentForDecisionEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.BudgetSentForDecisionFailedEvent;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.SendWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.dto.PrintableBudgetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private WorkOrderBudgetGateway gateway;
    @Mock
    private BudgetDecisionTokenGateway tokenGateway;
    @Mock
    private EmailService emailService;
    @Mock
    private GetPrintableBudgetUseCase getPrintableBudgetUseCase;
    @Mock
    private EventPublisher eventPublisher;

    private SendWorkOrderBudgetUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendWorkOrderBudgetUseCase(
                gateway, tokenGateway, emailService, getPrintableBudgetUseCase,
                eventPublisher, "/budget-decision/");
    }

    @Test
    @DisplayName("Deve enviar orçamento por e-mail e publicar evento")
    void shouldSendBudgetByEmail() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, "cliente@teste.com");
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));
        when(getPrintableBudgetUseCase.execute(any())).thenReturn(
                new PrintableBudgetResponse("orcamento.pdf",
                        Base64.getEncoder().encodeToString("pdf".getBytes())));

        useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api"));

        assertEquals(WorkOrderBudgetStatus.WAITING_DECISION, budget.getStatus());
        verify(gateway).save(budget);
        verify(tokenGateway).create(any(BudgetDecisionToken.class));
        verify(eventPublisher).publish(any(BudgetSentForDecisionEvent.class));
        verify(emailService).send(eq("cliente@teste.com"), anyString(), anyString(), any(EmailAttachment.class));
    }

    @Test
    @DisplayName("Deve pular o e-mail quando o cliente não tiver e-mail")
    void shouldSkipEmailWhenClientEmailMissing() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, null);
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api"));

        verify(gateway).save(budget);
        verify(tokenGateway).create(any(BudgetDecisionToken.class));
        verify(eventPublisher).publish(any(BudgetSentForDecisionEvent.class));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve publicar evento de falha quando o envio de e-mail falhar")
    void shouldPublishFailureEventWhenEmailFails() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, "cliente@teste.com");
        when(gateway.findById(workOrderId)).thenReturn(Optional.of(budget));
        when(getPrintableBudgetUseCase.execute(any())).thenReturn(
                new PrintableBudgetResponse("orcamento.pdf",
                        Base64.getEncoder().encodeToString("pdf".getBytes())));
        doThrow(new RuntimeException("smtp down"))
                .when(emailService).send(anyString(), anyString(), anyString(), any());

        assertThrows(RuntimeException.class,
                () -> useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api")));

        verify(eventPublisher).publish(any(BudgetSentForDecisionFailedEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não for encontrado")
    void shouldThrowWhenBudgetNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(gateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new SendWorkOrderBudgetCommand(workOrderId, "http://localhost:8080/api")));

        verify(gateway, never()).save(any());
        verifyNoInteractions(emailService, tokenGateway);
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
