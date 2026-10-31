package com.mecanicadm.mecanicadm_api.core.workorder.usecase;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.BudgetDecisionToken;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.BudgetDecisionTokenGateway;
import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.DecideWorkOrderBudgetCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.ProcessBudgetDecisionByTokenCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessBudgetDecisionByTokenUseCaseTest {

    @Mock
    private BudgetDecisionTokenGateway tokenGateway;

    @Mock
    private DecideWorkOrderBudgetUseCase decideWorkOrderBudgetUseCase;

    @InjectMocks
    private ProcessBudgetDecisionByTokenUseCase useCase;

    @Test
    @DisplayName("Deve processar aprovação via token com sucesso")
    void shouldProcessApprovalViaToken() {
        UUID workOrderId = UUID.randomUUID();
        String tokenValue = "test-token-123";
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), workOrderId, tokenValue, false, LocalDateTime.now());
        when(tokenGateway.findByToken(tokenValue)).thenReturn(Optional.of(token));

        useCase.execute(new ProcessBudgetDecisionByTokenCommand(tokenValue, "APPROVED", null));

        ArgumentCaptor<DecideWorkOrderBudgetCommand> captor =
                ArgumentCaptor.forClass(DecideWorkOrderBudgetCommand.class);
        verify(decideWorkOrderBudgetUseCase).execute(captor.capture());
        assertEquals(workOrderId, captor.getValue().workOrderId());
        assertEquals("APPROVED", captor.getValue().decision().name());
        assertNull(captor.getValue().observation());
        verify(tokenGateway).update(org.mockito.ArgumentMatchers.argThat(BudgetDecisionToken::isUsed));
    }

    @Test
    @DisplayName("Deve processar rejeição via token com motivo informado")
    void shouldProcessRejectionViaTokenWithReason() {
        String tokenValue = "test-token-456";
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), tokenValue, false, LocalDateTime.now());
        when(tokenGateway.findByToken(tokenValue)).thenReturn(Optional.of(token));

        String reason = "O valor está muito alto, preciso de uma cotação menor";
        useCase.execute(new ProcessBudgetDecisionByTokenCommand(tokenValue, "REJECTED", reason));

        ArgumentCaptor<DecideWorkOrderBudgetCommand> captor =
                ArgumentCaptor.forClass(DecideWorkOrderBudgetCommand.class);
        verify(decideWorkOrderBudgetUseCase).execute(captor.capture());
        assertEquals("REJECTED", captor.getValue().decision().name());
        assertEquals(reason, captor.getValue().observation());
    }

    @Test
    @DisplayName("Deve lançar exceção quando token não for encontrado")
    void shouldThrowExceptionWhenTokenNotFound() {
        when(tokenGateway.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetTokenNotFound.class,
                () -> useCase.execute(new ProcessBudgetDecisionByTokenCommand("invalid-token", "APPROVED", null)));

        verify(decideWorkOrderBudgetUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando token já foi utilizado")
    void shouldThrowExceptionWhenTokenAlreadyUsed() {
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "used-token", true, LocalDateTime.now().minusHours(1));
        when(tokenGateway.findByToken("used-token")).thenReturn(Optional.of(token));

        BudgetExceptions.BudgetTokenInvalid ex = assertThrows(BudgetExceptions.BudgetTokenInvalid.class,
                () -> useCase.execute(new ProcessBudgetDecisionByTokenCommand("used-token", "APPROVED", null)));
        assertEquals("budget.token.invalid", ex.getMessageKey());
        assertEquals("já utilizado", ex.getArgs()[0]);
        verify(decideWorkOrderBudgetUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando token estiver expirado")
    void shouldThrowExceptionWhenTokenExpired() {
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "expired-token", false, LocalDateTime.now().minusHours(25));
        when(tokenGateway.findByToken("expired-token")).thenReturn(Optional.of(token));

        BudgetExceptions.BudgetTokenInvalid ex = assertThrows(BudgetExceptions.BudgetTokenInvalid.class,
                () -> useCase.execute(new ProcessBudgetDecisionByTokenCommand("expired-token", "APPROVED", null)));
        assertEquals("budget.token.invalid", ex.getMessageKey());
        assertEquals("expirado", ex.getArgs()[0]);
        verify(decideWorkOrderBudgetUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Deve validar token válido com sucesso")
    void shouldValidateValidTokenSuccessfully() {
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "valid-token", false, LocalDateTime.now());
        when(tokenGateway.findByToken("valid-token")).thenReturn(Optional.of(token));

        assertDoesNotThrow(() -> useCase.validateToken("valid-token"));
        verify(tokenGateway).findByToken("valid-token");
    }

    @Test
    @DisplayName("Deve lançar exceção ao validar token não encontrado")
    void shouldThrowExceptionWhenValidateTokenNotFound() {
        when(tokenGateway.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThrows(BudgetExceptions.BudgetTokenNotFound.class,
                () -> useCase.validateToken("missing-token"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao validar token já utilizado")
    void shouldThrowExceptionWhenValidateTokenAlreadyUsed() {
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "used-token", true, LocalDateTime.now().minusHours(1));
        when(tokenGateway.findByToken("used-token")).thenReturn(Optional.of(token));

        BudgetExceptions.BudgetTokenInvalid ex = assertThrows(BudgetExceptions.BudgetTokenInvalid.class,
                () -> useCase.validateToken("used-token"));
        assertEquals("já utilizado", ex.getArgs()[0]);
    }

    @Test
    @DisplayName("Deve lançar exceção ao validar token expirado")
    void shouldThrowExceptionWhenValidateTokenExpired() {
        BudgetDecisionToken token = BudgetDecisionToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "expired-token", false, LocalDateTime.now().minusHours(25));
        when(tokenGateway.findByToken("expired-token")).thenReturn(Optional.of(token));

        BudgetExceptions.BudgetTokenInvalid ex = assertThrows(BudgetExceptions.BudgetTokenInvalid.class,
                () -> useCase.validateToken("expired-token"));
        assertEquals("expirado", ex.getArgs()[0]);
    }
}
