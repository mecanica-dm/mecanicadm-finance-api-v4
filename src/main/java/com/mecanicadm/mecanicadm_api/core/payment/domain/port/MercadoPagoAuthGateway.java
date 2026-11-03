package com.mecanicadm.mecanicadm_api.core.payment.domain.port;


import com.mecanicadm.mecanicadm_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_api.infra.features.payment.api.request.TokenRequest;

public interface MercadoPagoAuthGateway {
    AuthToken generateToken(TokenRequest request);
}
