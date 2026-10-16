package com.mecanicadm.mecanicadm_api.core.workorder.domain.port;

public interface WorkOrderExecutionDurationProjection {
    Object getWorkOrderId();
    Double getDurationMinutes();
}
