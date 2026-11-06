package com.mecanicadm.mecanicadm_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_api.core.shared.domain.port.InboxGateway;
import com.mecanicadm.mecanicadm_api.core.shared.usecase.ProcessInboundMessageUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(InboxProperties.class)
public class InboxConfiguration {

    @Bean
    public ProcessInboundMessageUseCase processInboundMessageUseCase(InboxGateway inboxGateway) {
        return new ProcessInboundMessageUseCase(inboxGateway);
    }
}
