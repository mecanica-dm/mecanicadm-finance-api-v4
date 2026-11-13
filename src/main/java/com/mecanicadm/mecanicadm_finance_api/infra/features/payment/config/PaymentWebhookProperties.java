package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.webhook")
public class PaymentWebhookProperties {

    private String secret = "";
    private boolean signatureRequired = true;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    public void setSignatureRequired(boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    public boolean hasSecret() {
        return secret != null && !secret.isBlank();
    }
}
