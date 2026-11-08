package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoAuthGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.TokenRequest;

public class GenerateMercadoPagoTokenUseCase {

    private final MercadoPagoAuthGateway authGateway;

    public GenerateMercadoPagoTokenUseCase(MercadoPagoAuthGateway authGateway) {
        this.authGateway = authGateway;
    }

    public AuthToken execute(TokenRequest request) {
        if (request.getGrantType() == null || request.getGrantType().isBlank()) {
            throw new PaymentException.PaymentGrantTypeRequired();
        }
        return authGateway.generateToken(request);
    }

    public AuthToken execute(String clientId, String clientSecret) {
        TokenRequest request = new TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        return execute(request);
    }
}