package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.VerifyPendingPaymentsUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config.PaymentWebhookProperties;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.webhook.MercadoPagoWebhookSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MercadoPagoWebhookController.PATH)
@Tag(name = "Pagamento", description = "Pedidos de pagamento no Mercado Pago")
public class MercadoPagoWebhookController {

    public static final String PATH = "/v1/payments/webhooks/mercado-pago";

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);
    private static final String ORDER_TYPE = "order";

    private final MercadoPagoWebhookSignatureValidator signatureValidator;
    private final VerifyPendingPaymentsUseCase verifyPendingPaymentsUseCase;
    private final PaymentWebhookProperties properties;

    public MercadoPagoWebhookController(MercadoPagoWebhookSignatureValidator signatureValidator,
                                        VerifyPendingPaymentsUseCase verifyPendingPaymentsUseCase,
                                        PaymentWebhookProperties properties) {
        this.signatureValidator = signatureValidator;
        this.verifyPendingPaymentsUseCase = verifyPendingPaymentsUseCase;
        this.properties = properties;
    }

    @PostMapping
    @Operation(summary = "Recebe a notificação (webhook) do Mercado Pago",
            description = "Rota pública, protegida pela assinatura (x-signature). Configure a URL em Suas integrações, Webhooks, evento Order. "
                    + "Responde 200 para qualquer notificação válida, inclusive as ignoradas; 401 para assinatura inválida; 503 se o segredo não foi configurado.")
    public ResponseEntity<Void> receive(@RequestHeader(value = "x-signature", required = false) String signature,
                                        @RequestHeader(value = "x-request-id", required = false) String requestId,
                                        @RequestParam(value = "data.id", required = false) String dataIdParam,
                                        @RequestParam(value = "type", required = false) String typeParam,
                                        @RequestBody(required = false) JsonNode body) {
        String dataId = dataIdParam != null ? dataIdParam : text(body, "data", "id");
        String type = typeParam != null ? typeParam : text(body, "type");

        if (properties.isSignatureRequired()) {
            if (!properties.hasSecret()) {
                logger.warn("Webhook do Mercado Pago recusado: MERCADO_PAGO_WEBHOOK_SECRET nao configurado");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            if (!signatureValidator.isValid(signature, requestId, dataId)) {
                logger.warn("Webhook do Mercado Pago recusado: assinatura invalida (data.id={})", dataId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (!ORDER_TYPE.equals(type) || dataId == null || dataId.isBlank()) {
            logger.info("Webhook do Mercado Pago ignorado (type={}, data.id={})", type, dataId);
            return ResponseEntity.ok().build();
        }

        try {
            boolean updated = verifyPendingPaymentsUseCase.verifyByOrderId(dataId);
            logger.info("Webhook do Mercado Pago processado para o pedido {} (resultado atualizado: {})", dataId, updated);
        } catch (RuntimeException e) {
            // O Mercado Pago repete a notificacao se nao receber 200; a rotina de verificacao tambem cobre este pagamento.
            logger.error("Falha ao processar o webhook do Mercado Pago para o pedido {}", dataId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }

    private static String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String field : path) {
            if (current == null) {
                return null;
            }
            current = current.get(field);
        }
        return current == null || current.isNull() ? null : current.asText();
    }
}
