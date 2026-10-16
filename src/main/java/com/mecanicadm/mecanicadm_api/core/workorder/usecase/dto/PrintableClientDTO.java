package com.mecanicadm.mecanicadm_api.core.workorder.usecase.dto;

public record PrintableClientDTO(
        String name,
        String document,
        String phone,
        String email
) {}
