package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.GetMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.openapi.MercadoPagoOrderOpenApi;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments/orders")
public class MercadoPagoOrderController implements MercadoPagoOrderOpenApi {

    private final CreateMercadoPagoOrderUseCase createOrderUseCase;
    private final GetMercadoPagoPaymentUseCase getPaymentUseCase;

    public MercadoPagoOrderController(CreateMercadoPagoOrderUseCase createOrderUseCase,
                                      GetMercadoPagoPaymentUseCase getPaymentUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @Override
    @PostMapping
    public ResponseEntity<MercadoPagoOrder> createOrder(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        String resolvedIdempotencyKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString()
                : idempotencyKey;

        Payment payment = createOrderUseCase.execute(request.toCommand(resolvedIdempotencyKey));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(payment.id())
                .toUri();

        return ResponseEntity.created(location).body(payment.providerResponse());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<MercadoPagoOrder> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(getPaymentUseCase.execute(id).providerResponse());
    }
}
