package com.mecanicadm.mecanicadm_api.core.vehicle.domain.port;

import com.mecanicadm.mecanicadm_api.core.vehicle.domain.Vehicle;

import java.util.List;

public record VehiclePageResult(List<Vehicle> items, long totalElements) {
}

