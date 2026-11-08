package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoOrderRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve serializar o pedido no contrato snake_case esperado pelo Mercado Pago")
    void shouldSerializeUsingMercadoPagoContract() throws Exception {
        MercadoPagoOrderRequest request = new MercadoPagoOrderRequest(
                "online",
                "external-reference",
                new MercadoPagoOrderRequest.Transactions(List.of(
                        new MercadoPagoOrderRequest.Payment(
                                "50.00",
                                new MercadoPagoOrderRequest.PaymentMethod(
                                        "visa", "credit_card", "card-token", 1, "MECANICA DM")
                        )
                )),
                new MercadoPagoOrderRequest.Payer("test_payer@example.com", "Joao", "Silva"),
                "50.00",
                "automatic",
                "automatic",
                "Pagamento Mecanica DM",
                new MercadoPagoOrderRequest.Config(
                        new MercadoPagoOrderRequest.Location("BR-SP"),
                        new MercadoPagoOrderRequest.Online(
                                new MercadoPagoOrderRequest.TransactionSecurity("never"))
                )
        );

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"external_reference\":\"external-reference\""));
        assertTrue(json.contains("\"transactions\":{\"payments\":[{"));
        assertTrue(json.contains("\"total_amount\":\"50.00\""));
        assertTrue(json.contains("\"payment_method\":{"));
        assertTrue(json.contains("\"installments\":1"));
        assertTrue(json.contains("\"state_id\":\"BR-SP\""));
        assertTrue(json.contains("\"transaction_security\":{\"validation\":\"never\"}"));
        assertFalse(json.contains("externalReference"));
        assertFalse(json.contains("totalAmount"));
    }
}
