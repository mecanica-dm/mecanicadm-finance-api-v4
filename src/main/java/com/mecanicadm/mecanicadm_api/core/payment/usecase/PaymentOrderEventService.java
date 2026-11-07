package com.mecanicadm.mecanicadm_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_api.core.payment.domain.event.PaymentConfirmedEvent;
import com.mecanicadm.mecanicadm_api.core.payment.domain.event.PaymentFailedEvent;
import com.mecanicadm.mecanicadm_api.core.payment.domain.event.PaymentLinkGenerationFailedEvent;
import com.mecanicadm.mecanicadm_api.core.payment.domain.event.PaymentLinkGeneratedEvent;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.EventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class PaymentOrderEventService {

    private static final Set<String> CONFIRMED_STATUSES =
            Set.of("approved", "authorized", "completed", "paid", "processed");
    private static final Set<String> FAILED_STATUSES =
            Set.of("canceled", "cancelled", "expired", "failed", "rejected");

    private final EventPublisher eventPublisher;

    public PaymentOrderEventService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishLinkGenerated(Payment payment, UUID workOrderId) {
        eventPublisher.publish(new PaymentLinkGeneratedEvent(
                workOrderId,
                payment.id(),
                resolvePaymentLink(payment),
                payment.amount(),
                null,
                null
        ));
    }

    public void publishOutcome(Payment payment, UUID workOrderId) {
        String status = resolveStatus(payment);

        if (CONFIRMED_STATUSES.contains(status)) {
            eventPublisher.publish(new PaymentConfirmedEvent(
                    workOrderId,
                    payment.id(),
                    payment.mercadoPagoPaymentId(),
                    payment.amount(),
                    null,
                    null
            ));
            return;
        }

        if (FAILED_STATUSES.contains(status)) {
            eventPublisher.publish(new PaymentFailedEvent(
                    workOrderId,
                    payment.id(),
                    payment.mercadoPagoPaymentId(),
                    payment.amount(),
                    resolveFailureReason(payment),
                    null,
                    null
            ));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishLinkGenerationFailed(UUID workOrderId, String externalReference, String reason) {
        eventPublisher.publish(new PaymentLinkGenerationFailedEvent(
                workOrderId,
                externalReference,
                reason,
                null,
                null
        ));
    }

    private String resolveStatus(Payment payment) {
        MercadoPagoOrder.Payment providerPayment = firstPayment(payment);
        String status = providerPayment == null ? null : providerPayment.getStatus();

        if (status == null || status.isBlank()) {
            status = payment.status();
        }

        return status == null ? "" : status.toLowerCase(Locale.ROOT);
    }

    private String resolveFailureReason(Payment payment) {
        MercadoPagoOrder.Payment providerPayment = firstPayment(payment);
        String reason = providerPayment == null ? null : providerPayment.getStatusDetail();

        if (reason == null || reason.isBlank()) {
            reason = payment.statusDetail();
        }

        if (reason == null || reason.isBlank()) {
            reason = payment.status();
        }

        return reason;
    }

    private String resolvePaymentLink(Payment payment) {
        MercadoPagoOrder.Payment providerPayment = firstPayment(payment);
        if (providerPayment == null || providerPayment.getPaymentMethod() == null) {
            return null;
        }
        return providerPayment.getPaymentMethod().getTicketUrl();
    }

    private MercadoPagoOrder.Payment firstPayment(Payment payment) {
        MercadoPagoOrder order = payment.providerResponse();
        if (order == null || order.getTransactions() == null) {
            return null;
        }

        List<MercadoPagoOrder.Payment> payments = order.getTransactions().getPayments();
        if (payments == null || payments.isEmpty()) {
            return null;
        }

        return payments.get(0);
    }
}
