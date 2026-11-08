package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetItem;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetVehicle;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableBudgetResponse;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.query.GetPrintableBudgetQuery;
import com.mecanicadm.mecanicadm_finance_api.infra.pdf.PdfGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPrintableBudgetUseCaseTest {

    @Mock
    private WorkOrderBudgetGateway budgetGateway;
    @Mock
    private PdfGenerator pdfGenerator;

    @InjectMocks
    private GetPrintableBudgetUseCase useCase;

    @Test
    @DisplayName("Deve gerar PDF a partir dos dados do orçamento")
    void shouldGeneratePdfFromBudgetInfo() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, client(), vehicle());
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.of(budget));
        byte[] pdf = "pdf-content".getBytes();
        when(pdfGenerator.generatePdfFromHtml(eq("budget-template"), anyMap())).thenReturn(pdf);

        PrintableBudgetResponse response = useCase.execute(new GetPrintableBudgetQuery(workOrderId));

        assertEquals("orcamento-os-" + workOrderId.toString().substring(0, 8) + ".pdf", response.fileName());
        assertEquals(Base64.getEncoder().encodeToString(pdf), response.base64Content());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o orçamento não for encontrado")
    void shouldThrowWhenBudgetNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetNotFound.class,
                () -> useCase.execute(new GetPrintableBudgetQuery(workOrderId)));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o cliente não estiver no orçamento")
    void shouldThrowWhenClientMissing() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, null, vehicle());
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        assertThrows(BudgetExceptions.ClientRequired.class,
                () -> useCase.execute(new GetPrintableBudgetQuery(workOrderId)));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o veículo não estiver no orçamento")
    void shouldThrowWhenVehicleMissing() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrderBudget budget = budget(workOrderId, client(), null);
        when(budgetGateway.findById(workOrderId)).thenReturn(Optional.of(budget));

        assertThrows(BudgetExceptions.VehicleRequired.class,
                () -> useCase.execute(new GetPrintableBudgetQuery(workOrderId)));
    }

    private WorkOrderBudget budget(UUID workOrderId, WorkOrderBudgetClient client, WorkOrderBudgetVehicle vehicle) {
        WorkOrderBudgetInfo info = new WorkOrderBudgetInfo(client, vehicle, "descricao", Set.of(
                WorkOrderBudgetItem.create("Material", 2, new BigDecimal("50.00"), WorkOrderBudgetItem.TYPE_MATERIAL),
                WorkOrderBudgetItem.create("Servico", 1, new BigDecimal("80.00"), WorkOrderBudgetItem.TYPE_LABOR)
        ));
        return WorkOrderBudget.restore(
                workOrderId, new BigDecimal("180.00"), WorkOrderBudgetStatus.PENDING, null, info);
    }

    private WorkOrderBudgetClient client() {
        return WorkOrderBudgetClient.create("Cliente", "cliente@teste.com", "12345678901", "48999999999");
    }

    private WorkOrderBudgetVehicle vehicle() {
        return WorkOrderBudgetVehicle.create("ABC-1234", "Modelo", "Marca", (short) 2020);
    }
}
