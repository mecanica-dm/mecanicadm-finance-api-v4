package com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event;

import java.util.UUID;

public interface MessageHeader {
    UUID messageId();

    UUID workOrderId();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
