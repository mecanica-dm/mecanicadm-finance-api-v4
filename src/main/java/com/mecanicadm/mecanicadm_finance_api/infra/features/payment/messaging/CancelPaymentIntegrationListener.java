package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.CancelPaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CancelMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CancelMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox.InboundMessageContext;
import com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CancelPaymentIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(CancelPaymentIntegrationListener.class);

    private final CancelMercadoPagoPaymentUseCase useCase;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public CancelPaymentIntegrationListener(CancelMercadoPagoPaymentUseCase useCase,
                                            RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.useCase = useCase;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(CancelPaymentIntegrationCommand command) {
        logger.info("Recebido CancelPaymentIntegrationCommand para a OS {}", command.workOrderId());

        try {
            useCase.execute(new CancelMercadoPagoPaymentCommand(command.workOrderId()));
        } catch (DomainExceptionCore e) {
            rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                    command.workOrderId(),
                    CancelPaymentIntegrationCommand.class.getSimpleName(),
                    e,
                    InboundMessageContext.current()));
            throw e;
        }
    }
}
