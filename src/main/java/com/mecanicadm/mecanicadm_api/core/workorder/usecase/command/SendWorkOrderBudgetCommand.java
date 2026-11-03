package com.mecanicadm.mecanicadm_api.core.workorder.usecase.command;

import java.util.UUID;

public record SendWorkOrderBudgetCommand(UUID workOrderId, String baseUrl) { }
