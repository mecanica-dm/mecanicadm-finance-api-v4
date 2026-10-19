package com.mecanicadm.mecanicadm_api.core.vehicle.domain.port;

public record VehiclePageQuery(
        VehicleFilter filter,
        int page,
        int size,
        String sortBy,
        String direction
) {
}



