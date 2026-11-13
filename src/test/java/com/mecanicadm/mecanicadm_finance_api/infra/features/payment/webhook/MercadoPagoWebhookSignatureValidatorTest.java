package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.webhook;

import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config.PaymentWebhookProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoWebhookSignatureValidatorTest {

    private static final String SECRET = "segredo-do-webhook";
    private static final String TS = "1742505638683";
    private static final String REQUEST_ID = "req-123";
    private static final String DATA_ID = "ORD01M28P44G5FG8RJPM579EH56FV";

    private final PaymentWebhookProperties properties = properties(SECRET);
    private final MercadoPagoWebhookSignatureValidator validator = new MercadoPagoWebhookSignatureValidator(properties);

    private static PaymentWebhookProperties properties(String secret) {
        PaymentWebhookProperties p = new PaymentWebhookProperties();
        p.setSecret(secret);
        return p;
    }

    private static String hmac(String secret, String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }

    private static String header(String ts, String v1) {
        return "ts=" + ts + ",v1=" + v1;
    }

    @Test
    @DisplayName("Deve aceitar a assinatura do manifesto com o data.id em minusculas")
    void shouldAcceptLowercaseDataIdManifest() throws Exception {
        String v1 = hmac(SECRET, "id:" + DATA_ID.toLowerCase() + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";");

        assertThat(validator.isValid(header(TS, v1), REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar a assinatura do manifesto com o data.id como veio")
    void shouldAcceptOriginalCaseDataIdManifest() throws Exception {
        String v1 = hmac(SECRET, "id:" + DATA_ID + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";");

        assertThat(validator.isValid(header(TS, v1), REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    @DisplayName("Deve tirar do manifesto os pares ausentes (sem request-id e sem data.id)")
    void shouldOmitMissingPairs() throws Exception {
        String withoutRequestId = hmac(SECRET, "id:" + DATA_ID.toLowerCase() + ";ts:" + TS + ";");
        String onlyTimestamp = hmac(SECRET, "ts:" + TS + ";");

        assertThat(validator.isValid(header(TS, withoutRequestId), null, DATA_ID)).isTrue();
        assertThat(validator.isValid(header(TS, onlyTimestamp), "", null)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar espacos e hash em maiusculas no cabecalho")
    void shouldToleratePaddingAndUppercaseHash() throws Exception {
        String v1 = hmac(SECRET, "id:" + DATA_ID.toLowerCase() + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";").toUpperCase();

        assertThat(validator.isValid(" ts=" + TS + " , v1=" + v1 + " ", REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    @DisplayName("Deve recusar assinatura feita com outro segredo, ou com dados alterados")
    void shouldRefuseWrongSecretOrTamperedData() throws Exception {
        String other = hmac("outro-segredo", "id:" + DATA_ID.toLowerCase() + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";");
        String valid = hmac(SECRET, "id:" + DATA_ID.toLowerCase() + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";");

        assertThat(validator.isValid(header(TS, other), REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValid(header(TS, valid), REQUEST_ID, "ORDOUTRO")).isFalse();
        assertThat(validator.isValid(header(TS, valid), "req-outro", DATA_ID)).isFalse();
        assertThat(validator.isValid(header("1", valid), REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Deve recusar cabecalho ausente ou malformado")
    void shouldRefuseMissingOrMalformedHeader() {
        assertThat(validator.isValid(null, REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValid("  ", REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValid("lixo", REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValid("ts=" + TS, REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValid("v1=abc", REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValid("ts=,v1=", REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Sem segredo configurado, nenhuma assinatura vale")
    void shouldRefuseEverythingWithoutSecret() throws Exception {
        MercadoPagoWebhookSignatureValidator noSecret = new MercadoPagoWebhookSignatureValidator(properties(""));
        String v1 = hmac(SECRET, "id:" + DATA_ID.toLowerCase() + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";");

        assertThat(noSecret.isValid(header(TS, v1), REQUEST_ID, DATA_ID)).isFalse();
    }
}
