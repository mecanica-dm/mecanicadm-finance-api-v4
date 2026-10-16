package com.mecanicadm.mecanicadm_api.core.workorder.domain.port;

public interface LaborExecutionSummaryProjection {
    Long getTotalProcessedLabors();
    Double getAverageExecutionMinutes();
}
