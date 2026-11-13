package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.PaymentStatuses;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.VerifyPendingPaymentsCommand;
import com.mecanicadm.mecanicadm_finance_api.shared.usecase.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class VerifyPendingPaymentsUseCase implements UseCase<VerifyPendingPaymentsCommand, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(VerifyPendingPaymentsUseCase.class);

    private final PaymentGateway paymentGateway;
    private final MercadoPagoOrderGateway orderGateway;
    private final MercadoPagoAccessTokenProvider accessTokenProvider;
    private final PaymentOrderEventService paymentOrderEventService;

    public VerifyPendingPaymentsUseCase(PaymentGateway paymentGateway,
                                        MercadoPagoOrderGateway orderGateway,
                                        MercadoPagoAccessTokenProvider accessTokenProvider,
                                        PaymentOrderEventService paymentOrderEventService) {
        this.paymentGateway = paymentGateway;
        this.orderGateway = orderGateway;
        this.accessTokenProvider = accessTokenProvider;
        this.paymentOrderEventService = paymentOrderEventService;
    }

    @Override
    @Transactional
    public Integer execute(VerifyPendingPaymentsCommand command) {
        List<Payment> pending = paymentGateway.findPendingForVerification(
                Instant.now().minus(command.minAge()), command.batchSize());
        if (pending.isEmpty()) {
            return 0;
        }

        String accessToken = accessTokenProvider.accessToken();
        int changed = 0;
        for (Payment payment : pending) {
            if (verify(payment, accessToken)) {
                changed++;
            }
        }
        return changed;
    }

    @Transactional
    public boolean verifyByOrderId(String mercadoPagoOrderId) {
        Payment payment = paymentGateway.findByMercadoPagoOrderIdForUpdate(mercadoPagoOrderId).orElse(null);
        if (payment == null) {
            logger.info("Webhook do Mercado Pago para o pedido {}, que nao e de nenhum pagamento deste servico; ignorado", mercadoPagoOrderId);
            return false;
        }
        if (PaymentStatuses.FINAL.contains(PaymentStatuses.effective(payment))) {
            return false;
        }
        return verify(payment, accessTokenProvider.accessToken());
    }

    private boolean verify(Payment payment, String accessToken) {
        MercadoPagoOrder order;
        try {
            order = orderGateway.getOrder(payment.mercadoPagoOrderId(), accessToken);
        } catch (RuntimeException e) {
            logger.warn("Nao foi possivel consultar o pedido {} no Mercado Pago: {}", payment.mercadoPagoOrderId(), e.getMessage());
            return false;
        }

        String previous = PaymentStatuses.effective(payment);
        String status = order.getStatus() == null || order.getStatus().isBlank() ? payment.status() : order.getStatus();
        Payment updated = paymentGateway.save(payment.withProviderUpdate(status, order));
        String current = PaymentStatuses.effective(updated);
        if (current.equals(previous)) {
            return false;
        }

        UUID workOrderId = workOrderIdOf(payment.externalReference());
        paymentOrderEventService.publishOutcome(updated, workOrderId);
        return true;
    }

    private UUID workOrderIdOf(String externalReference) {
        try {
            return UUID.fromString(externalReference);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
