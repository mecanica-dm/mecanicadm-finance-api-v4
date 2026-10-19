package com.mecanicadm.mecanicadm_api.core.workorder.domain.port;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrder;

import java.util.List;

public record WorkOrderPageResult(
        List<WorkOrder> items, long totalElements
) {
}
