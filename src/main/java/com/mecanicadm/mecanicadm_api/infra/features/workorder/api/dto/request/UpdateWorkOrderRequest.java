package com.mecanicadm.mecanicadm_api.infra.features.workorder.api.dto.request;

import java.util.UUID;

public record UpdateWorkOrderRequest(
        String vehicleId,
        UUID clientId,
        String description
) {
}
