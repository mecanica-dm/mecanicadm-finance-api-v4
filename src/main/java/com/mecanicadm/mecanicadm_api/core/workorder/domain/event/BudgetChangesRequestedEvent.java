package com.mecanicadm.mecanicadm_api.core.workorder.domain.event;

import java.util.UUID;

public class BudgetChangesRequestedEvent {
    private UUID workOrderId;
    private String observation;
}
