package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoCredentials;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MercadoPagoAccessTokenProviderTest {

    private final GenerateMercadoPagoTokenUseCase generateToken = mock(GenerateMercadoPagoTokenUseCase.class);

    @Test
    @DisplayName("Deve usar o access token configurado sem chamar o Mercado Pago")
    void shouldUseConfiguredAccessToken() {
        MercadoPagoAccessTokenProvider provider = new MercadoPagoAccessTokenProvider(generateToken,
                new MercadoPagoCredentials("tok", null, null, null));

        assertThat(provider.accessToken()).isEqualTo("tok");
        verifyNoInteractions(generateToken);
    }

    @Test
    @DisplayName("Deve gerar o token com as credenciais do aplicativo quando nao houver access token")
    void shouldGenerateTokenFromClientCredentials() {
        AuthToken token = new AuthToken();
        token.setAccessToken("gerado");
        when(generateToken.execute("id", "secret")).thenReturn(token);

        assertThat(new MercadoPagoAccessTokenProvider(generateToken, new MercadoPagoCredentials(null, null, "id", "secret")).accessToken())
                .isEqualTo("gerado");
    }

    @Test
    @DisplayName("Deve falhar sem access token nem credenciais, ou quando o Mercado Pago nao devolver token")
    void shouldFailWithoutCredentialsOrToken() {
        assertThatThrownBy(() -> new MercadoPagoAccessTokenProvider(generateToken, new MercadoPagoCredentials(null, null, null, null)).accessToken())
                .isInstanceOf(PaymentException.PaymentAccessTokenRequired.class);

        when(generateToken.execute("id", "secret")).thenReturn(new AuthToken());
        assertThatThrownBy(() -> new MercadoPagoAccessTokenProvider(generateToken, new MercadoPagoCredentials(null, null, "id", "secret")).accessToken())
                .isInstanceOf(PaymentException.PaymentAccessTokenRequired.class);
    }
}
