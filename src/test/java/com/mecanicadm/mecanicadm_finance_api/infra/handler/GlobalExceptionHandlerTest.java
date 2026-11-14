package com.mecanicadm.mecanicadm_finance_api.infra.handler;

import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.infra.security.exception.InvalidTokenException;
import com.mecanicadm.mecanicadm_finance_api.shared.exception.TechnicalException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final MessageSource messages = mock(MessageSource.class);
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages);
    private final Locale locale = Locale.of("pt", "BR");

    @Test
    @DisplayName("Excecao de dominio deve responder com o status dela e a mensagem traduzida")
    void shouldMapDomainException() {
        when(messages.getMessage(eq("payment.not.found"), any(), anyString(), eq(locale))).thenReturn("Pagamento nao encontrado");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(new PaymentException.PaymentNotFound(), locale);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Pagamento nao encontrado");
    }

    @Test
    @DisplayName("Excecao tecnica e excecao generica devem responder 500")
    void shouldMapTechnicalAndGenericExceptionsTo500() {
        when(messages.getMessage(anyString(), any(), anyString(), eq(locale))).thenReturn("erro");

        assertThat(handler.handleTechnicalException(new TechnicalException("x.y"), locale).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(handler.handleGenericException(new IllegalStateException("boom"), locale).getBody()).containsEntry("error", "erro");
    }

    @Test
    @DisplayName("Falha de seguranca e credenciais invalidas devem responder 401")
    void shouldMapSecurityFailuresTo401() {
        when(messages.getMessage(anyString(), any(), anyString(), eq(locale))).thenReturn("nao autorizado");
        when(messages.getMessage(eq("error.bad.credentials"), any(), eq(locale))).thenReturn("credenciais invalidas");

        assertThat(handler.handleSecurityException(new InvalidTokenException(), locale).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ResponseEntity<Map<String, String>> bad = handler.handleBadCredentialsException(new BadCredentialsException("x"), locale);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(bad.getBody()).containsEntry("error", "credenciais invalidas");
    }

    @Test
    @DisplayName("Validacao de corpo deve responder 400 com a mensagem de cada campo, e a padrao quando faltar")
    void shouldMapBodyValidationErrors() {
        when(messages.getMessage(eq("validation.error.unknown"), any(), anyString(), eq(locale))).thenReturn("erro desconhecido");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "email", "email invalido"));
        binding.addError(new FieldError("obj", "nome", null, false, null, null, null));

        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(
                new MethodArgumentNotValidException(null, binding), locale);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("email", "email invalido").containsEntry("nome", "erro desconhecido");
    }

    @Test
    @DisplayName("Violacao de restricao deve responder 400, traduzindo a mensagem entre chaves")
    void shouldMapConstraintViolations() {
        when(messages.getMessage(eq("campo.obrigatorio"), any(), anyString(), eq(locale))).thenReturn("Campo obrigatorio");
        ConstraintViolation<?> translated = violation("nome", "{campo.obrigatorio}");
        ConstraintViolation<?> plain = violation("idade", "deve ser maior que zero");

        ResponseEntity<Map<String, String>> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(translated, plain)), locale);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("nome", "Campo obrigatorio").containsEntry("idade", "deve ser maior que zero");
    }

    private ConstraintViolation<?> violation(String property, String message) {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(property);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }
}
