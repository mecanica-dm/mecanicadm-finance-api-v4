package com.mecanicadm.mecanicadm_api.core.material.domain.port;

import com.mecanicadm.mecanicadm_api.core.material.domain.enums.MaterialType;

public record MaterialFilter(
        String name,
        String brand,
        MaterialType type) {
}
