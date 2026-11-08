package com.mecanicadm.mecanicadm_finance_api.infra.messaging.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
