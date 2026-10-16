package com.mecanicadm.mecanicadm_api.core.workorder.domain.port;

public interface WorkOrderExecutionSummaryProjection {
    Long getTotalProcessedWorkOrders();
    Double getAverageExecutionMinutes();
}
