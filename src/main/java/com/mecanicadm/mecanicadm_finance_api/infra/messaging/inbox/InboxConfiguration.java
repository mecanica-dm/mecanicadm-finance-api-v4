package com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox;

import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.InboxGateway;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.ProcessInboundMessageUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandService;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
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

    @Bean
    public RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase(EventPublisher eventPublisher) {
        return new RejeitarFinanceCommandService(eventPublisher);
    }
}
