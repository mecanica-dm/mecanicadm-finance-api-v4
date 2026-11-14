package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.RefundPaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.RefundMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.RefundMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.messaging.inbox.InboundMessageContext;
import com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RefundPaymentIntegrationListener {

    private static final Logger logger = LoggerFactory.getLogger(RefundPaymentIntegrationListener.class);

    private final RefundMercadoPagoPaymentUseCase useCase;
    private final RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    public RefundPaymentIntegrationListener(RefundMercadoPagoPaymentUseCase useCase,
                                            RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase) {
        this.useCase = useCase;
        this.rejeitarFinanceCommandUseCase = rejeitarFinanceCommandUseCase;
    }

    public void onMessage(RefundPaymentIntegrationCommand command) {
        logger.info("Recebido RefundPaymentIntegrationCommand para a OS {}", command.workOrderId());

        try {
            useCase.execute(new RefundMercadoPagoPaymentCommand(command.workOrderId()));
        } catch (DomainExceptionCore e) {
            rejeitarFinanceCommandUseCase.execute(new RejeitarFinanceCommandCommand(
                    command.workOrderId(),
                    RefundPaymentIntegrationCommand.class.getSimpleName(),
                    e,
                    InboundMessageContext.current()));
            throw e;
        }
    }
}
