package com.mecanicadm.mecanicadm_api.infra.features.workorder.api.dto.response;

import java.util.UUID;

public record WorkOrderExecutionTimeInfoResponse(
        UUID workOrderId,
        Double durationInMinutes
) {
}

