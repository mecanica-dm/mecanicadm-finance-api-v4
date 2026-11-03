package com.mecanicadm.mecanicadm_api.infra.messaging.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
