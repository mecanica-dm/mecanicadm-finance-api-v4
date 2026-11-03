package com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoAuthGateway;
import com.mecanicadm.mecanicadm_api.infra.features.payment.api.request.TokenRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MercadoPagoAuthGatewayImpl implements MercadoPagoAuthGateway {

    private final RestClient restClient;

    public MercadoPagoAuthGatewayImpl(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.mercadopago.com").build();
    }

    @Override
    public AuthToken generateToken(TokenRequest request) {
        return restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AuthToken.class);
    }
}