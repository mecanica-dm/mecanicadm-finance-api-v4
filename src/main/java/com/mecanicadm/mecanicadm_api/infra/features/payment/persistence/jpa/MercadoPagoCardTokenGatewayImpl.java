package com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoCardTokenGateway;
import com.mecanicadm.mecanicadm_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.jpa.dto.MercadoPagoCardTokenRequest;
import com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.jpa.dto.MercadoPagoCardTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MercadoPagoCardTokenGatewayImpl implements MercadoPagoCardTokenGateway {

    private static final String BASE_URL = "https://api.mercadopago.com";

    private final RestClient restClient;

    public MercadoPagoCardTokenGatewayImpl(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String createCardToken(CardData cardData, String publicKey) {
        try {
            MercadoPagoCardTokenResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/v1/card_tokens")
                            .queryParam("public_key", publicKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toRequest(cardData))
                    .retrieve()
                    .body(MercadoPagoCardTokenResponse.class);

            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new PaymentException.PaymentCardTokenNotGenerated();
            }

            return response.id();
        } catch (RestClientResponseException ex) {
            throw new PaymentException.MercadoPagoProviderError(
                    ex.getStatusCode().value(), sanitizeDetail(ex.getResponseBodyAsString()));
        } catch (NumberFormatException ex) {
            throw new PaymentException.PaymentCardDataInvalid();
        }
    }

    private MercadoPagoCardTokenRequest toRequest(CardData cardData) {
        return new MercadoPagoCardTokenRequest(
                cardData.number(),
                Integer.valueOf(cardData.expirationMonth()),
                Integer.valueOf(cardData.expirationYear()),
                cardData.securityCode(),
                new MercadoPagoCardTokenRequest.Cardholder(
                        cardData.holderName(),
                        new MercadoPagoCardTokenRequest.Identification(
                                cardData.identificationType(),
                                cardData.identificationNumber()
                        )
                )
        );
    }

    private String sanitizeDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return "Erro não informado pelo Mercado Pago";
        }
        return detail.length() > 500 ? detail.substring(0, 500) : detail;
    }
}
