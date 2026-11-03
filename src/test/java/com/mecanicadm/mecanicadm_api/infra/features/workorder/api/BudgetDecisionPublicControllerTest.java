package com.mecanicadm.mecanicadm_api.infra.features.workorder.api;

import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.ProcessBudgetDecisionByTokenUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetDecisionPublicControllerTest {

    @Mock
    private ProcessBudgetDecisionByTokenUseCase processBudgetDecisionByTokenUseCase;

    private BudgetDecisionPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new BudgetDecisionPublicController(processBudgetDecisionByTokenUseCase);
    }

    @Test
    @DisplayName("Deve retornar formulário de aprovação com token válido")
    void shouldReturnFormPageForApproval() {
        var response = controller.form("valid-token", "APPROVED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("Aprovar Orçamento"));
        assertTrue(response.getBody().contains("valid-token"));
        verify(processBudgetDecisionByTokenUseCase).validateToken("valid-token");
    }

    @Test
    @DisplayName("Deve retornar formulário de rejeição com required")
    void shouldReturnFormPageForRejection() {
        var response = controller.form("valid-token", "REJECTED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Rejeitar Orçamento"));
        assertTrue(response.getBody().contains("required"));
    }

    @Test
    @DisplayName("Deve retornar formulário de solicitação de alterações")
    void shouldReturnFormPageForChangesRequested() {
        var response = controller.form("valid-token", "CHANGES_REQUESTED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Solicitar Alterações"));
        assertTrue(response.getBody().contains("required"));
    }

    @Test
    @DisplayName("Deve retornar formulário para ação desconhecida")
    void shouldReturnFormPageForUnknownAction() {
        var response = controller.form("valid-token", "OTHER");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Resposta ao Orçamento"));
    }

    @Test
    @DisplayName("Deve retornar página de erro quando token já foi utilizado")
    void shouldReturnErrorPageWhenTokenAlreadyUsed() {
        doThrow(BudgetExceptions.BudgetTokenInvalid.of("já utilizado"))
                .when(processBudgetDecisionByTokenUseCase).validateToken("used-token");

        var response = controller.form("used-token", "APPROVED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Link já utilizado"));
    }

    @Test
    @DisplayName("Deve retornar página de erro quando token não for encontrado")
    void shouldReturnErrorPageWhenTokenNotFound() {
        doThrow(new BudgetExceptions.BudgetTokenNotFound())
                .when(processBudgetDecisionByTokenUseCase).validateToken("invalid-token");

        var response = controller.form("invalid-token", "APPROVED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Link inválido"));
    }

    @Test
    @DisplayName("Deve processar decisão com sucesso")
    void shouldProcessDecisionSuccessfully() {
        var response = controller.decide("valid-token", "APPROVED", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Orçamento aprovado com sucesso!"));
        verify(processBudgetDecisionByTokenUseCase).execute(any());
    }

    @Test
    @DisplayName("Deve processar rejeição com observação")
    void shouldProcessRejectionWithObservation() {
        var response = controller.decide("valid-token", "REJECTED", "Valor muito alto");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Orçamento rejeitado"));
        verify(processBudgetDecisionByTokenUseCase).execute(any());
    }

    @Test
    @DisplayName("Deve retornar página de erro ao decidir com token inválido")
    void shouldReturnErrorPageWhenDecideWithInvalidToken() {
        doThrow(BudgetExceptions.BudgetTokenInvalid.of("já utilizado"))
                .when(processBudgetDecisionByTokenUseCase).execute(any());

        var response = controller.decide("used-token", "APPROVED", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Link já utilizado"));
    }

    @Test
    @DisplayName("Deve retornar página de erro ao decidir com token inexistente")
    void shouldReturnErrorPageWhenDecideWithNotFoundToken() {
        doThrow(new BudgetExceptions.BudgetTokenNotFound())
                .when(processBudgetDecisionByTokenUseCase).execute(any());

        var response = controller.decide("nonexistent-token", "APPROVED", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Link inválido"));
    }
}
