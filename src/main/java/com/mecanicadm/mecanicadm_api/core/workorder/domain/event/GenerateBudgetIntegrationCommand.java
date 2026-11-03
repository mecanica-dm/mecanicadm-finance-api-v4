package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerateBudgetIntegrationCommand(UUID workOrderId, BigDecimal totalPrice) { }
