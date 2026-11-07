package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MercadoPagoCardTokenRequest(
        String cardNumber,
        Integer expirationMonth,
        Integer expirationYear,
        String securityCode,
        Cardholder cardholder
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Cardholder(String name, Identification identification) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Identification(String type, String number) {
    }
}
