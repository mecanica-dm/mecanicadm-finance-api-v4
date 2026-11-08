package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request;

import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import jakarta.validation.constraints.NotBlank;

public record SimulateOrderRequest(
        @NotBlank(message = "{validation.payment.order.externalreference.required}")
        String externalReference
) {

    public CreateMercadoPagoOrderCommand toCommand(String idempotencyKey) {
        return new CreateMercadoPagoOrderCommand(
                idempotencyKey,
                externalReference,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
