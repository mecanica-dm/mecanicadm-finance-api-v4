package com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrderData;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.jpa.dto.MercadoPagoOrderRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class MercadoPagoOrderGatewayImpl implements MercadoPagoOrderGateway {

    private static final String BASE_URL = "https://api.mercadopago.com";
    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String TYPE_ONLINE = "online";
    private static final String PROCESSING_MODE_AUTOMATIC = "automatic";
    private static final String CAPTURE_MODE_AUTOMATIC = "automatic";
    private static final String STATE_ID_SAO_PAULO = "BR-SP";
    private static final String TRANSACTION_SECURITY_NEVER = "never";
    private static final String STATEMENT_DESCRIPTOR = "MECANICA DM";

    private final RestClient restClient;

    public MercadoPagoOrderGatewayImpl(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public MercadoPagoOrder createOrder(MercadoPagoOrderData orderData, String accessToken, String idempotencyKey) {
        try {
            MercadoPagoOrder order = restClient.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                    .body(toRequest(orderData))
                    .retrieve()
                    .body(MercadoPagoOrder.class);

            if (order == null) {
                throw new PaymentException.MercadoPagoProviderError(502, "Resposta vazia do Mercado Pago");
            }

            return order;
        } catch (RestClientResponseException ex) {
            throw new PaymentException.MercadoPagoProviderError(
                    ex.getStatusCode().value(), sanitizeDetail(ex.getResponseBodyAsString()));
        }
    }

    private MercadoPagoOrderRequest toRequest(MercadoPagoOrderData orderData) {
        MercadoPagoOrderRequest.PaymentMethod paymentMethod = new MercadoPagoOrderRequest.PaymentMethod(
                orderData.paymentMethodId(),
                orderData.paymentMethodType(),
                orderData.cardToken(),
                orderData.installments(),
                STATEMENT_DESCRIPTOR
        );

        MercadoPagoOrderRequest.Payment payment = new MercadoPagoOrderRequest.Payment(
                orderData.totalAmount(),
                paymentMethod
        );

        MercadoPagoOrderRequest.Payer payer = new MercadoPagoOrderRequest.Payer(
                orderData.payerEmail(),
                orderData.payerFirstName(),
                orderData.payerLastName()
        );

        MercadoPagoOrderRequest.Config config = new MercadoPagoOrderRequest.Config(
                new MercadoPagoOrderRequest.Location(STATE_ID_SAO_PAULO),
                new MercadoPagoOrderRequest.Online(
                        new MercadoPagoOrderRequest.TransactionSecurity(TRANSACTION_SECURITY_NEVER))
        );

        return new MercadoPagoOrderRequest(
                TYPE_ONLINE,
                orderData.externalReference(),
                new MercadoPagoOrderRequest.Transactions(List.of(payment)),
                payer,
                orderData.totalAmount(),
                CAPTURE_MODE_AUTOMATIC,
                PROCESSING_MODE_AUTOMATIC,
                orderData.description(),
                config
        );
    }

    private String sanitizeDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return "Erro não informado pelo Mercado Pago";
        }
        return detail.length() > 500 ? detail.substring(0, 500) : detail;
    }
}
