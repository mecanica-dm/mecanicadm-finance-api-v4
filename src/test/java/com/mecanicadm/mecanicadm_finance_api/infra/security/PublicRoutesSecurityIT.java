package com.mecanicadm.mecanicadm_finance_api.infra.security;

import com.mecanicadm.mecanicadm_finance_api.testutils.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicRoutesSecurityIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("A pagina de decisao do orcamento, aberta pelo link do e-mail, deve ser acessivel sem JWT")
    void shouldServeBudgetDecisionPageWithoutJwt() throws Exception {
        mockMvc.perform(get("/budget-decision/token-inexistente/form").param("action", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Link inv")));
    }

    @Test
    @DisplayName("O webhook do Mercado Pago deve ser publico (sem JWT): sem segredo configurado, responde 503 e nao 401")
    void shouldServeWebhookWithoutJwt() throws Exception {
        mockMvc.perform(post("/v1/payments/webhooks/mercado-pago").param("data.id", "ORD1").param("type", "order"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("As rotas internas do orcamento e do pagamento devem continuar exigindo JWT")
    void shouldRequireJwtForInternalRoutes() throws Exception {
        mockMvc.perform(post("/budgets/{id}/send", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/payments/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
