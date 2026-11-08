package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CalculateWorkOrderBudgetCommand(
        UUID workOrderId,
        BigDecimal totalPrice
) {
}
