package com.mecanicadm.mecanicadm_api.core.labor.domain.port;

public record LaborPageQuery(LaborFilter filter, int page, int size, String sortBy, String direction) {
}

