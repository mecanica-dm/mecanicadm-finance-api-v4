package com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.event.MessageHeader;

public interface EventPublisher {
    void publish(MessageHeader event);
}
