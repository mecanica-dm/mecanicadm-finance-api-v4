package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String externalReference,
        String description,
        @Positive(message = "{validation.payment.order.totalamount.positive}")
        BigDecimal totalAmount,
        @Email(message = "{validation.payment.order.payeremail.invalid}")
        String payerEmail,
        String payerFirstName,
        String payerLastName,
        @Min(value = 1, message = "{validation.payment.order.installments.invalid}")
        @Max(value = 36, message = "{validation.payment.order.installments.invalid}")
        Integer installments,
        String paymentMethodId,
        String paymentMethodType,
        CardRequest card
) {

    public CreateMercadoPagoOrderCommand toCommand(String idempotencyKey) {
        return new CreateMercadoPagoOrderCommand(
                idempotencyKey,
                externalReference,
                description,
                totalAmount,
                payerEmail,
                payerFirstName,
                payerLastName,
                installments,
                paymentMethodId,
                paymentMethodType,
                card == null ? null : card.toCardData()
        );
    }

    public record CardRequest(
            String number,
            String expirationMonth,
            String expirationYear,
            String securityCode,
            String holderName,
            String identificationType,
            String identificationNumber
    ) {

        public CardData toCardData() {
            return new CardData(
                    number,
                    expirationMonth,
                    expirationYear,
                    securityCode,
                    holderName,
                    identificationType,
                    identificationNumber
            );
        }
    }
}
