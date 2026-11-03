package com.mecanicadm.mecanicadm_api.core.payment.domain;

public record CardData(
        String number,
        String expirationMonth,
        String expirationYear,
        String securityCode,
        String holderName,
        String identificationType,
        String identificationNumber
) {

    public CardData merge(CardData fallback) {
        if (fallback == null) {
            return this;
        }

        return new CardData(
                valueOrDefault(number, fallback.number()),
                valueOrDefault(expirationMonth, fallback.expirationMonth()),
                valueOrDefault(expirationYear, fallback.expirationYear()),
                valueOrDefault(securityCode, fallback.securityCode()),
                valueOrDefault(holderName, fallback.holderName()),
                valueOrDefault(identificationType, fallback.identificationType()),
                valueOrDefault(identificationNumber, fallback.identificationNumber())
        );
    }

    public boolean isComplete() {
        return hasText(number)
                && hasText(expirationMonth)
                && hasText(expirationYear)
                && hasText(securityCode)
                && hasText(holderName)
                && hasText(identificationType)
                && hasText(identificationNumber);
    }

    private static String valueOrDefault(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
