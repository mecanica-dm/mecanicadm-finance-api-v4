package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public class BudgetGeneratedEvent {
    private UUID workOrderId;
    private BigDecimal totalPrice;
}
