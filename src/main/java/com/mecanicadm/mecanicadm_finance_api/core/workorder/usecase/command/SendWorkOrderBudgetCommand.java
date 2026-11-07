package com.mecanicadm.mecanicadm_finance_api.core.workorder.usecase.command;

import java.util.UUID;

public record SendWorkOrderBudgetCommand(UUID workOrderId, String baseUrl) {
}
