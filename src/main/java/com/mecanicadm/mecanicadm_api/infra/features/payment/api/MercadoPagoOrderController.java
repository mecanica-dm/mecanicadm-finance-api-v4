package com.mecanicadm.mecanicadm_api.infra.features.payment.api;

import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_api.infra.features.payment.api.request.CreateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments/orders")
@Tag(name = "Pagamentos", description = "Endpoints para pagamentos com o Mercado Pago")
public class MercadoPagoOrderController {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final CreateMercadoPagoOrderUseCase createOrderUseCase;

    public MercadoPagoOrderController(CreateMercadoPagoOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    @Operation(summary = "Criar pedido de pagamento",
            description = "Cria um pedido de pagamento com cartão de crédito no Mercado Pago. "
                    + "Quando os dados do cartão não são enviados, são utilizados os valores padrão de teste.")
    @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "502", description = "Erro na integração com o Mercado Pago")
    public ResponseEntity<MercadoPagoOrder> createOrder(
            @Parameter(description = "Chave de idempotência. Quando não informada, uma chave é gerada automaticamente.")
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        String resolvedIdempotencyKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString()
                : idempotencyKey;

        MercadoPagoOrder order = createOrderUseCase.execute(request.toCommand(resolvedIdempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
