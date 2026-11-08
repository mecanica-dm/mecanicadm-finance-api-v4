package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetClient;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetInfo;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetItem;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.WorkOrderBudgetVehicle;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableBudgetDTO;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableBudgetResponse;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableClientDTO;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableLaborItemDTO;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableMaterialItemDTO;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.dto.PrintableVehicleDTO;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.query.GetPrintableBudgetQuery;
import com.mecanicadm.mecanicadm_finance_api.infra.pdf.PdfGenerator;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.UseCase;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GetPrintableBudgetUseCase implements UseCase<GetPrintableBudgetQuery, PrintableBudgetResponse> {
    private final WorkOrderBudgetGateway budgetGateway;
    private final PdfGenerator pdfGenerator;

    public GetPrintableBudgetUseCase(WorkOrderBudgetGateway budgetGateway, PdfGenerator pdfGenerator) {
        this.budgetGateway = budgetGateway;
        this.pdfGenerator = pdfGenerator;
    }

    public PrintableBudgetResponse execute(GetPrintableBudgetQuery query) {
        WorkOrderBudget budget = getBudget(query.workOrderId());
        WorkOrderBudgetInfo info = budget.getInfo();

        PrintableClientDTO clientDTO = buildClientDTO(info.getClient());
        PrintableVehicleDTO vehicleDTO = buildVehicleDTO(info.getVehicle());

        List<PrintableLaborItemDTO> laborItems = buildLaborItems(info.getItems());
        List<PrintableMaterialItemDTO> materialItems = buildMaterialItems(info.getItems());

        BigDecimal totalLabor = calculateTotalLabor(laborItems);
        BigDecimal totalMaterial = calculateTotalMaterial(materialItems);

        PrintableBudgetDTO budgetDTO = new PrintableBudgetDTO(
                clientDTO,
                vehicleDTO,
                laborItems,
                materialItems,
                totalLabor,
                totalMaterial,
                budget.getTotalPrice()
        );

        return generatePdfResponse(budget.getWorkOrderId(), budgetDTO);
    }

    private WorkOrderBudget getBudget(UUID workOrderId) {
        return budgetGateway.findById(workOrderId)
                .orElseThrow(BudgetExceptions.BudgetNotFound::new);
    }

    private PrintableClientDTO buildClientDTO(WorkOrderBudgetClient client) {
        if (client == null) {
            throw new BudgetExceptions.ClientRequired();
        }

        return new PrintableClientDTO(
                client.getName(),
                formatDocument(client.getDocument()),
                formatPhone(client.getPhone()),
                client.getEmail()
        );
    }

    private PrintableVehicleDTO buildVehicleDTO(WorkOrderBudgetVehicle vehicle) {
        if (vehicle == null) {
            throw new BudgetExceptions.VehicleRequired();
        }

        return new PrintableVehicleDTO(
                vehicle.getLicensePlate(),
                vehicle.getModel()
        );
    }

    private List<PrintableLaborItemDTO> buildLaborItems(Set<WorkOrderBudgetItem> items) {
        return items.stream()
                .filter(item -> WorkOrderBudgetItem.TYPE_LABOR.equals(item.getType()))
                .map(item -> new PrintableLaborItemDTO(item.getDescription(), item.getPrice()))
                .toList();
    }

    private List<PrintableMaterialItemDTO> buildMaterialItems(Set<WorkOrderBudgetItem> items) {
        return items.stream()
                .filter(item -> WorkOrderBudgetItem.TYPE_MATERIAL.equals(item.getType()))
                .map(item -> {
                    BigDecimal unitPrice = item.getPrice();
                    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new PrintableMaterialItemDTO(
                            item.getDescription(),
                            item.getQuantity(),
                            unitPrice,
                            totalPrice
                    );
                })
                .toList();
    }

    private BigDecimal calculateTotalLabor(List<PrintableLaborItemDTO> laborItems) {
        return laborItems.stream()
                .map(PrintableLaborItemDTO::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalMaterial(List<PrintableMaterialItemDTO> materialItems) {
        return materialItems.stream()
                .map(PrintableMaterialItemDTO::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PrintableBudgetResponse generatePdfResponse(UUID workOrderId, PrintableBudgetDTO budgetDTO) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("budget", budgetDTO);

        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml("budget-template", variables);
        String base64Content = Base64.getEncoder().encodeToString(pdfBytes);
        String fileName = "orcamento-os-" + workOrderId.toString().substring(0, 8) + ".pdf";

        return new PrintableBudgetResponse(fileName, base64Content);
    }

    private String formatDocument(String document) {
        if (document == null) return "";
        String numbers = document.replaceAll("\\D", "");
        if (numbers.length() == 11) {
            return numbers.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else if (numbers.length() == 14) {
            return numbers.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return document;
    }

    private String formatPhone(String phone) {
        if (phone == null) return "";
        String numbers = phone.replaceAll("\\D", "");
        if (numbers.length() == 11) {
            return numbers.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        } else if (numbers.length() == 10) {
            return numbers.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }
        return phone;
    }
}
