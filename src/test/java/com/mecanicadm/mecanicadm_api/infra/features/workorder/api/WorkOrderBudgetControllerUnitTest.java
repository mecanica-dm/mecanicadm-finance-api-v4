package com.mecanicadm.mecanicadm_api.infra.features.workorder.api;

import com.mecanicadm.mecanicadm_api.core.workorder.usecase.CalculateWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.DecideWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.GetPrintableBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.ManuallyAdjustWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.SendWorkOrderBudgetUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.dto.PrintableBudgetResponse;
import com.mecanicadm.mecanicadm_api.infra.features.workorder.api.dto.request.DecideWorkOrderBudgetRequest;
import com.mecanicadm.mecanicadm_api.infra.features.workorder.api.dto.request.ManuallyAdjustWorkOrderBudgetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderBudgetControllerUnitTest {

    @Mock
    private SendWorkOrderBudgetUseCase sendWorkOrderBudgetUseCase;
    @Mock
    private ManuallyAdjustWorkOrderBudgetUseCase manuallyAdjustWorkOrderBudgetUseCase;
    @Mock
    private DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase;
    @Mock
    private CalculateWorkOrderBudgetUseCase calculateWorkOrderBudgetUseCase;
    @Mock
    private GetPrintableBudgetUseCase getPrintableBudgetUseCase;

    @InjectMocks
    private WorkOrderBudgetController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:8080/api");
    }

    @Test
    @DisplayName("Deve enviar orçamento e retornar 204")
    void shouldSendBudgetAndReturn204() {
        var response = controller.sendBudget(UUID.randomUUID());

        assertEquals(204, response.getStatusCode().value());
        verify(sendWorkOrderBudgetUseCase).execute(any());
    }

    @Test
    @DisplayName("Deve ajustar orçamento e retornar 204")
    void shouldAdjustBudgetAndReturn204() {
        var request = new ManuallyAdjustWorkOrderBudgetRequest(BigDecimal.valueOf(500));

        var response = controller.adjustBudget(UUID.randomUUID(), request);

        assertEquals(204, response.getStatusCode().value());
        verify(manuallyAdjustWorkOrderBudgetUseCase).execute(any());
    }

    @Test
    @DisplayName("Deve recalcular orçamento e retornar 204")
    void shouldRecalculateBudgetAndReturn204() {
        var response = controller.recalculateBudget(UUID.randomUUID());

        assertEquals(204, response.getStatusCode().value());
        verify(calculateWorkOrderBudgetUseCase).execute(any());
    }

    @Test
    @DisplayName("Deve decidir orçamento e retornar 204")
    void shouldDecideBudgetAndReturn204() {
        var request = new DecideWorkOrderBudgetRequest(
                com.mecanicadm.mecanicadm_api.core.workorder.domain.enums.WorkOrderBudgetStatus.APPROVED, null);

        var response = controller.decideBudget(UUID.randomUUID(), request);

        assertEquals(204, response.getStatusCode().value());
        verify(decideWorkOrderBudgetUseCase).execute(any());
    }

    @Test
    @DisplayName("Deve imprimir orçamento e retornar 200")
    void shouldPrintBudgetAndReturn200() {
        PrintableBudgetResponse pdfResponse = new PrintableBudgetResponse("budget.pdf", "base64content");
        when(getPrintableBudgetUseCase.execute(any())).thenReturn(pdfResponse);

        var response = controller.printBudget(UUID.randomUUID());

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("budget.pdf", response.getBody().fileName());
    }
}
