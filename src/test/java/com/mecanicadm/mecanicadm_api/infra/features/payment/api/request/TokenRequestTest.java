package com.mecanicadm.mecanicadm_api.infra.features.payment.api.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve serializar o token request no contrato snake_case esperado pelo Mercado Pago")
    void shouldSerializeUsingMercadoPagoContract() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setGrantType("client_credentials");
        request.setClientId("client-id");
        request.setClientSecret("client-secret");

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"client_id\":\"client-id\""));
        assertTrue(json.contains("\"client_secret\":\"client-secret\""));
        assertTrue(json.contains("\"grant_type\":\"client_credentials\""));
        assertFalse(json.contains("clientId"));
        assertFalse(json.contains("clientSecret"));
        assertFalse(json.contains("grantType"));
    }

    @Test
    @DisplayName("Deve aceitar camelCase e snake_case na desserialização")
    void shouldAcceptCamelCaseAndSnakeCase() throws Exception {
        TokenRequest camelCase = objectMapper.readValue(
                "{\"clientId\":\"id\",\"clientSecret\":\"secret\",\"grantType\":\"client_credentials\"}",
                TokenRequest.class);
        TokenRequest snakeCase = objectMapper.readValue(
                "{\"client_id\":\"id\",\"client_secret\":\"secret\",\"grant_type\":\"client_credentials\"}",
                TokenRequest.class);

        assertEquals("id", camelCase.getClientId());
        assertEquals("secret", camelCase.getClientSecret());
        assertEquals("client_credentials", camelCase.getGrantType());
        assertEquals("id", snakeCase.getClientId());
        assertEquals("secret", snakeCase.getClientSecret());
        assertEquals("client_credentials", snakeCase.getGrantType());
    }

    @Test
    @DisplayName("Não deve serializar campos nulos")
    void shouldNotSerializeNullFields() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setGrantType("client_credentials");

        String json = objectMapper.writeValueAsString(request);

        assertFalse(json.contains("client_id"));
        assertFalse(json.contains("client_secret"));
        assertTrue(json.contains("\"grant_type\":\"client_credentials\""));
    }
}
