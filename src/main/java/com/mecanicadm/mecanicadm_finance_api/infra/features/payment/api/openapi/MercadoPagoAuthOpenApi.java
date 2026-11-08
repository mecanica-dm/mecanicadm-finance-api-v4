package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.openapi;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.TokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação de Pagamentos", description = "Endpoints para autenticação com o Mercado Pago")
public interface MercadoPagoAuthOpenApi {

    @Operation(summary = "Gerar token de acesso",
            description = "Gera um token de acesso no Mercado Pago a partir das credenciais e do tipo de concessão informados. "
                    + "O tipo de concessão (grantType) é obrigatório.")
    @ApiResponse(responseCode = "200", description = "Token gerado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    @ApiResponse(responseCode = "502", description = "Erro na integração com o Mercado Pago", content = @Content)
    ResponseEntity<AuthToken> generateToken(TokenRequest request);
}
