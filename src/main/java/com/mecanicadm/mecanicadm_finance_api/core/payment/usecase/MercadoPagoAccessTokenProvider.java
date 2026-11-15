package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoCredentials;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;

public class MercadoPagoAccessTokenProvider {

    private final GenerateMercadoPagoTokenUseCase generateTokenUseCase;
    private final MercadoPagoCredentials credentials;

    public MercadoPagoAccessTokenProvider(GenerateMercadoPagoTokenUseCase generateTokenUseCase, MercadoPagoCredentials credentials) {
        this.generateTokenUseCase = generateTokenUseCase;
        this.credentials = credentials;
    }

    public String accessToken() {
        if (credentials.hasAccessToken()) {
            return credentials.accessToken();
        }
        if (!credentials.hasClientCredentials()) {
            throw new PaymentException.PaymentAccessTokenRequired();
        }
        AuthToken authToken = generateTokenUseCase.execute(credentials.clientId(), credentials.clientSecret());
        if (authToken == null || authToken.getAccessToken() == null || authToken.getAccessToken().isBlank()) {
            throw new PaymentException.PaymentAccessTokenRequired();
        }
        return authToken.getAccessToken();
    }
}
