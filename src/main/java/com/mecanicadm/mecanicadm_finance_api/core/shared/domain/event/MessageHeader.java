package com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event;

import java.time.ZonedDateTime;
import java.util.UUID;

public interface MessageHeader {
    UUID messageId();

    UUID workOrderId();

    ZonedDateTime ocurredAt();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
