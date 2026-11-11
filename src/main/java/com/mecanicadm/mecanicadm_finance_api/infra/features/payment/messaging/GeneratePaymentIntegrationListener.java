package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.GeneratePaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GeneratePaymentIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(GeneratePaymentIntegrationListener.class);

    private final CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public GeneratePaymentIntegrationListener(CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase,
                                               RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.createMercadoPagoOrderUseCase = createMercadoPagoOrderUseCase;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(GeneratePaymentIntegrationCommand command) {
        logger.info("Recebido GeneratePaymentIntegrationCommand para a OS {}", command.id());

        try {
            createMercadoPagoOrderUseCase.execute(toCommand(command));
        } catch (RuntimeException e) {
            logger.error("Falha ao processar GeneratePaymentIntegrationCommand para a OS {}: {}",
                    command.id(), e.getMessage(), e);
            rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                    resolveWorkOrderId(command.id()),
                    GeneratePaymentIntegrationCommand.class.getSimpleName(),
                    e));
            throw e;
        }
    }

    private CreateMercadoPagoOrderCommand toCommand(GeneratePaymentIntegrationCommand command) {
        return new CreateMercadoPagoOrderCommand(
                command.id(),
                command.id(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private UUID resolveWorkOrderId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
