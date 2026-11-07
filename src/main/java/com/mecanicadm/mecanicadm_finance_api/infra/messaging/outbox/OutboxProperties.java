package com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private boolean enabled = true;
    private long initialDelayMs = 5000;
    private long pollIntervalMs = 1000;
    private int batchSize = 50;
    private long processingTimeoutSeconds = 60;
    private int retentionDays = 30;
    private String cleanupCron = "0 0 3 * * *";

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

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getProcessingTimeoutSeconds() {
        return processingTimeoutSeconds;
    }

    public void setProcessingTimeoutSeconds(long processingTimeoutSeconds) {
        this.processingTimeoutSeconds = processingTimeoutSeconds;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getCleanupCron() {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }
}
