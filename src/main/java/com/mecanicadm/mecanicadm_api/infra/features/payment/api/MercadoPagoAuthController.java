package com.mecanicadm.mecanicadm_api.infra.features.payment.api;


import com.mecanicadm.mecanicadm_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.GenerateMercadoPagoTokenUseCase;
import com.mecanicadm.mecanicadm_api.infra.features.payment.api.request.TokenRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments/auth")
public class MercadoPagoAuthController {

    private final GenerateMercadoPagoTokenUseCase generateTokenUseCase;

    public MercadoPagoAuthController(GenerateMercadoPagoTokenUseCase generateTokenUseCase) {
        this.generateTokenUseCase = generateTokenUseCase;
    }

    @PostMapping("/token")
    public ResponseEntity<AuthToken> generateToken(@RequestBody TokenRequest request) {
        AuthToken token = generateTokenUseCase.execute(request);
        return ResponseEntity.ok(token);
    }
}