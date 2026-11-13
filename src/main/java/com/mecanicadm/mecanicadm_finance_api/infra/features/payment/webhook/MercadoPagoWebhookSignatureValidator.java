package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.webhook;

import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config.PaymentWebhookProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class MercadoPagoWebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PaymentWebhookProperties properties;

    public MercadoPagoWebhookSignatureValidator(PaymentWebhookProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String xSignature, String xRequestId, String dataId) {
        if (!properties.hasSecret() || xSignature == null || xSignature.isBlank()) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] keyValue = part.trim().split("=", 2);
            if (keyValue.length == 2) {
                if ("ts".equals(keyValue[0].trim())) {
                    ts = keyValue[1].trim();
                } else if ("v1".equals(keyValue[0].trim())) {
                    v1 = keyValue[1].trim();
                }
            }
        }
        if (ts == null || ts.isEmpty() || v1 == null || v1.isEmpty()) {
            return false;
        }

        byte[] received = v1.toLowerCase().getBytes(StandardCharsets.UTF_8);
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(dataId == null ? null : dataId.toLowerCase());
        candidates.add(dataId);
        for (String candidate : candidates) {
            byte[] expected = sign(manifest(candidate, xRequestId, ts)).getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(expected, received)) {
                return true;
            }
        }
        return false;
    }

    private String manifest(String dataId, String xRequestId, String ts) {
        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifest.append("request-id:").append(xRequestId).append(';');
        }
        return manifest.append("ts:").append(ts).append(';').toString();
    }

    private String sign(String manifest) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Nao foi possivel calcular a assinatura do webhook", e);
        }
    }
}
