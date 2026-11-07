package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ManuallyAdjustWorkOrderBudgetRequest(
        @NotNull(message = "{validation.budget.price.required}")
        @PositiveOrZero(message = "{validation.budget.price.positiveorzero}")
        BigDecimal newTotalPrice
) {
}
