package com.mecanicadm.mecanicadm_api.core.payment.domain;

public record MercadoPagoCredentials(
        String accessToken,
        String publicKey,
        String clientId,
        String clientSecret
) {

    public boolean hasAccessToken() {
        return hasText(accessToken);
    }

    public boolean hasPublicKey() {
        return hasText(publicKey);
    }

    public boolean hasClientCredentials() {
        return hasText(clientId) && hasText(clientSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
