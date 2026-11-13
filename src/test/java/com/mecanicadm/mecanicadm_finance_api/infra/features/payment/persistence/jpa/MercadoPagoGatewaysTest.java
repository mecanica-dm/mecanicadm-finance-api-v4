package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrderData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.TokenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoGatewaysTest {

    private static final String BASE = "https://api.mercadopago.com";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private MercadoPagoOrderData orderData() {
        return new MercadoPagoOrderData("ref-1", "Revisao", "720.00", "a@a.com", "Maria", "Silva", "visa", "credit_card", "card-token", 1);
    }

    private CardData card() {
        return new CardData("4235647728025682", "11", "2030", "123", "APRO", "CPF", "19119119100");
    }

    @Test
    @DisplayName("Pedido: deve criar a ordem enviando token de acesso e chave de idempotencia")
    void shouldCreateOrder() {
        server.expect(requestTo(BASE + "/v1/orders")).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok")).andExpect(header("X-Idempotency-Key", "key-1"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ref-1")))
                .andRespond(withSuccess("{\"id\":\"ORD1\",\"status\":\"processed\"}", MediaType.APPLICATION_JSON));

        MercadoPagoOrder order = new MercadoPagoOrderGatewayImpl(builder).createOrder(orderData(), "tok", "key-1");

        assertThat(order.getId()).isEqualTo("ORD1");
        assertThat(order.getStatus()).isEqualTo("processed");
        server.verify();
    }

    @Test
    @DisplayName("Pedido: deve consultar a ordem pelo id, so com o token de acesso")
    void shouldGetOrder() {
        server.expect(requestTo(BASE + "/v1/orders/ORD1")).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("{\"id\":\"ORD1\",\"status\":\"processed\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1/orders/ORD2")).andRespond(withStatus(HttpStatus.NOT_FOUND).body("not_found"));
        server.expect(requestTo(BASE + "/v1/orders/ORD3")).andRespond(withSuccess());
        MercadoPagoOrderGatewayImpl gateway = new MercadoPagoOrderGatewayImpl(builder);

        assertThat(gateway.getOrder("ORD1", "tok").getStatus()).isEqualTo("processed");
        assertThatThrownBy(() -> gateway.getOrder("ORD2", "tok")).isInstanceOfSatisfying(
                PaymentException.MercadoPagoProviderError.class, e -> assertThat(e.getStatus()).isEqualTo(404));
        assertThatThrownBy(() -> gateway.getOrder("ORD3", "tok")).isInstanceOfSatisfying(
                PaymentException.MercadoPagoProviderError.class, e -> assertThat(e.getStatus()).isEqualTo(502));
        server.verify();
    }

    @Test
    @DisplayName("Pedido: deve cancelar e estornar pelos caminhos da API de Orders, sem corpo")
    void shouldCancelAndRefundOrder() {
        server.expect(requestTo(BASE + "/v1/orders/ORD1/cancel")).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "cancel-1")).andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("{\"id\":\"ORD1\",\"status\":\"canceled\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1/orders/ORD1/refund")).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "refund-1"))
                .andRespond(withSuccess("{\"id\":\"ORD1\",\"status\":\"processed\",\"status_detail\":\"refunded\"}", MediaType.APPLICATION_JSON));
        MercadoPagoOrderGatewayImpl gateway = new MercadoPagoOrderGatewayImpl(builder);

        assertThat(gateway.cancelOrder("ORD1", "tok", "cancel-1").getStatus()).isEqualTo("canceled");
        assertThat(gateway.refundOrder("ORD1", "tok", "refund-1").getStatusDetail()).isEqualTo("refunded");
        server.verify();
    }

    @Test
    @DisplayName("Pedido: erro do Mercado Pago deve virar MercadoPagoProviderError com o status e o detalhe")
    void shouldMapProviderErrors() {
        server.expect(requestTo(BASE + "/v1/orders")).andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"invalid\"]}"));
        server.expect(requestTo(BASE + "/v1/orders/ORD1/cancel")).andRespond(withStatus(HttpStatus.CONFLICT).body("order_already_canceled"));
        server.expect(requestTo(BASE + "/v1/orders/ORD1/refund")).andRespond(withStatus(HttpStatus.CONFLICT).body(""));
        MercadoPagoOrderGatewayImpl gateway = new MercadoPagoOrderGatewayImpl(builder);

        assertThatThrownBy(() -> gateway.createOrder(orderData(), "tok", "k")).isInstanceOfSatisfying(
                PaymentException.MercadoPagoProviderError.class, e -> assertThat(e.getStatus()).isEqualTo(400));
        assertThatThrownBy(() -> gateway.cancelOrder("ORD1", "tok", "k")).isInstanceOfSatisfying(
                PaymentException.MercadoPagoProviderError.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(409);
                    assertThat(e.getArgs()[0]).isEqualTo("order_already_canceled");
                });
        assertThatThrownBy(() -> gateway.refundOrder("ORD1", "tok", "k")).isInstanceOfSatisfying(
                PaymentException.MercadoPagoProviderError.class, e -> assertThat(e.getArgs()[0]).isEqualTo("Erro não informado pelo Mercado Pago"));
    }

    @Test
    @DisplayName("Pedido: resposta vazia do Mercado Pago deve ser recusada com 502")
    void shouldRefuseEmptyResponse() {
        server.expect(requestTo(BASE + "/v1/orders/ORD1/cancel")).andRespond(withSuccess());

        assertThatThrownBy(() -> new MercadoPagoOrderGatewayImpl(builder).cancelOrder("ORD1", "tok", "k"))
                .isInstanceOfSatisfying(PaymentException.MercadoPagoProviderError.class, e -> assertThat(e.getStatus()).isEqualTo(502));
    }

    @Test
    @DisplayName("Detalhe de erro muito longo deve ser cortado em 500 caracteres")
    void shouldTruncateLongErrorDetail() {
        server.expect(requestTo(BASE + "/v1/orders/ORD1/cancel")).andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("x".repeat(900)));

        assertThatThrownBy(() -> new MercadoPagoOrderGatewayImpl(builder).cancelOrder("ORD1", "tok", "k"))
                .isInstanceOfSatisfying(PaymentException.MercadoPagoProviderError.class,
                        e -> assertThat(String.valueOf(e.getArgs()[0])).hasSize(500));
    }

    @Test
    @DisplayName("Token de cartao: deve criar o token com a chave publica na query")
    void shouldCreateCardToken() {
        server.expect(requestTo(BASE + "/v1/card_tokens?public_key=pub")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"card-1\"}", MediaType.APPLICATION_JSON));

        assertThat(new MercadoPagoCardTokenGatewayImpl(builder).createCardToken(card(), "pub")).isEqualTo("card-1");
    }

    @Test
    @DisplayName("Token de cartao: resposta sem id, erro do provedor e mes invalido devem ser recusados")
    void shouldRefuseInvalidCardTokenResponses() {
        server.expect(requestTo(BASE + "/v1/card_tokens?public_key=pub")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1/card_tokens?public_key=pub")).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("nao autorizado"));
        MercadoPagoCardTokenGatewayImpl gateway = new MercadoPagoCardTokenGatewayImpl(builder);

        assertThatThrownBy(() -> gateway.createCardToken(card(), "pub")).isInstanceOf(PaymentException.PaymentCardTokenNotGenerated.class);
        assertThatThrownBy(() -> gateway.createCardToken(card(), "pub")).isInstanceOfSatisfying(
                PaymentException.MercadoPagoProviderError.class, e -> assertThat(e.getStatus()).isEqualTo(401));
        CardData invalid = new CardData("1", "mes", "2030", "123", "APRO", "CPF", "1");
        assertThatThrownBy(() -> gateway.createCardToken(invalid, "pub")).isInstanceOf(PaymentException.PaymentCardDataInvalid.class);
    }

    @Test
    @DisplayName("Autenticacao: deve trocar as credenciais por um token em /oauth/token")
    void shouldGenerateAuthToken() {
        server.expect(requestTo(BASE + "/oauth/token")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"abc\",\"token_type\":\"bearer\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        TokenRequest request = new TokenRequest();
        request.setClientId("id");
        request.setClientSecret("secret");
        request.setGrantType("client_credentials");

        AuthToken token = new MercadoPagoAuthGatewayImpl(builder).generateToken(request);

        assertThat(token.getAccessToken()).isEqualTo("abc");
        assertThat(token.getExpiresIn()).isEqualTo(3600L);
    }
}
