package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event;

import java.util.UUID;

public record CancelBudgetIntegrationCommand(UUID workOrderId) {
}
