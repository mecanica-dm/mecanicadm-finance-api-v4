package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoCardTokenResponse(String id) {
}
