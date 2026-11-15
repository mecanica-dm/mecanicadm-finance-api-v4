package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.verification")
public class PaymentVerificationProperties {

    private boolean enabled = true;
    private long initialDelayMs = 60000;
    private long intervalMs = 60000;
    private long minAgeSeconds = 60;
    private int batchSize = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public long getMinAgeSeconds() {
        return minAgeSeconds;
    }

    public void setMinAgeSeconds(long minAgeSeconds) {
        this.minAgeSeconds = minAgeSeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
