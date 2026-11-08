package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.openapi;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.CreateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@Tag(name = "Pagamentos", description = "Endpoints para pagamentos com o Mercado Pago")
public interface MercadoPagoOrderOpenApi {

    String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    @Operation(summary = "Criar pedido de pagamento",
            description = "Cria um pedido de pagamento com cartão de crédito no Mercado Pago. "
                    + "Quando os dados do cartão não são enviados, são utilizados os valores padrão de teste.")
    @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    @ApiResponse(responseCode = "502", description = "Erro na integração com o Mercado Pago", content = @Content)
    ResponseEntity<MercadoPagoOrder> createOrder(
            @Parameter(description = "Chave de idempotência. Quando não informada, uma chave é gerada automaticamente.")
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request);

    @Operation(summary = "Consultar pedido de pagamento",
            description = "Retorna o pedido de pagamento persistido a partir do identificador interno.")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    ResponseEntity<MercadoPagoOrder> findById(
            @Parameter(description = "Identificador interno do pedido") @PathVariable UUID id);
}
