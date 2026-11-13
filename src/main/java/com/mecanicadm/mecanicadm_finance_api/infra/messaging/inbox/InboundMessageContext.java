package com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox;

import java.util.UUID;

public final class InboundMessageContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private InboundMessageContext() {
    }

    public static void set(UUID messageId) {
        CURRENT.set(messageId);
    }

    public static UUID current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
