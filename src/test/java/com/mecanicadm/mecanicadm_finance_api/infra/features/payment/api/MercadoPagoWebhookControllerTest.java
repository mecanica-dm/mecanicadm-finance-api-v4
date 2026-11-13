package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api;

import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.VerifyPendingPaymentsUseCase;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config.PaymentWebhookProperties;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.webhook.MercadoPagoWebhookSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MercadoPagoWebhookControllerTest {

    private static final String PATH = "/v1/payments/webhooks/mercado-pago";
    private static final String BODY = "{\"action\":\"order.processed\",\"type\":\"order\",\"data\":{\"id\":\"ORD1\",\"status\":\"processed\"}}";

    private final MercadoPagoWebhookSignatureValidator validator = mock(MercadoPagoWebhookSignatureValidator.class);
    private final VerifyPendingPaymentsUseCase useCase = mock(VerifyPendingPaymentsUseCase.class);
    private final PaymentWebhookProperties properties = new PaymentWebhookProperties();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties.setSecret("segredo");
        mockMvc = MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController(validator, useCase, properties)).build();
    }

    @Test
    @DisplayName("Com assinatura valida, deve conferir o pedido do data.id da query e responder 200")
    void shouldVerifyOrderWithValidSignature() throws Exception {
        when(validator.isValid("ts=1,v1=abc", "req-1", "ORD1")).thenReturn(true);

        mockMvc.perform(post(PATH).param("data.id", "ORD1").param("type", "order").header("x-signature", "ts=1,v1=abc")
                        .header("x-request-id", "req-1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        verify(useCase).verifyByOrderId("ORD1");
    }

    @Test
    @DisplayName("Sem data.id e type na query, deve ler os dois do corpo")
    void shouldFallBackToBodyFields() throws Exception {
        when(validator.isValid(anyString(), any(), eq("ORD1"))).thenReturn(true);

        mockMvc.perform(post(PATH).header("x-signature", "ts=1,v1=abc").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        verify(useCase).verifyByOrderId("ORD1");
    }

    @Test
    @DisplayName("Assinatura invalida deve responder 401 e nao consultar nada")
    void shouldRespond401ForInvalidSignature() throws Exception {
        when(validator.isValid(any(), any(), any())).thenReturn(false);

        mockMvc.perform(post(PATH).param("data.id", "ORD1").param("type", "order").header("x-signature", "ts=1,v1=ruim")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verify(useCase, never()).verifyByOrderId(anyString());
    }

    @Test
    @DisplayName("Sem o segredo configurado, deve responder 503 e nao processar")
    void shouldRespond503WithoutSecret() throws Exception {
        properties.setSecret("");

        mockMvc.perform(post(PATH).param("data.id", "ORD1").param("type", "order").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable());

        verify(useCase, never()).verifyByOrderId(anyString());
    }

    @Test
    @DisplayName("Notificacao de outro tipo (ou sem id) deve responder 200 sem consultar nada")
    void shouldIgnoreOtherTypesAndMissingId() throws Exception {
        when(validator.isValid(any(), any(), any())).thenReturn(true);

        mockMvc.perform(post(PATH).param("data.id", "PAY1").param("type", "payment").header("x-signature", "ts=1,v1=abc"))
                .andExpect(status().isOk());
        mockMvc.perform(post(PATH).param("type", "order").header("x-signature", "ts=1,v1=abc"))
                .andExpect(status().isOk());

        verify(useCase, never()).verifyByOrderId(anyString());
    }

    @Test
    @DisplayName("Com a assinatura desligada (so em desenvolvimento), deve processar sem validar")
    void shouldProcessWithoutValidationWhenSignatureIsNotRequired() throws Exception {
        properties.setSecret("");
        properties.setSignatureRequired(false);

        mockMvc.perform(post(PATH).param("data.id", "ORD1").param("type", "order")).andExpect(status().isOk());

        verify(validator, never()).isValid(any(), any(), any());
        verify(useCase).verifyByOrderId("ORD1");
    }

    @Test
    @DisplayName("Falha ao processar deve responder 500, para o Mercado Pago repetir a notificacao")
    void shouldRespond500WhenProcessingFails() throws Exception {
        when(validator.isValid(any(), any(), any())).thenReturn(true);
        when(useCase.verifyByOrderId("ORD1")).thenThrow(new IllegalStateException("banco fora"));

        mockMvc.perform(post(PATH).param("data.id", "ORD1").param("type", "order").header("x-signature", "ts=1,v1=abc"))
                .andExpect(status().isInternalServerError());
    }
}
