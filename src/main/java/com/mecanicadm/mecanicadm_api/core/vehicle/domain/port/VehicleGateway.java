package com.mecanicadm.mecanicadm_api.core.vehicle.domain.port;

import com.mecanicadm.mecanicadm_api.core.vehicle.domain.Vehicle;

import java.util.Optional;

public interface VehicleGateway {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
}
