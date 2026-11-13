package com.mecanicadm.mecanicadm_finance_api.core.payment.domain;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PaymentStatuses {

    public static final String CANCELED = "canceled";
    public static final String REFUNDED = "refunded";

    private static final Set<String> CONFIRMED = Set.of("approved", "authorized", "completed", "paid", "processed");

    private static final Set<String> FAILED = Set.of("canceled", "cancelled", "expired", "failed", "rejected");

    public static final Set<String> FINAL = Stream.of(CONFIRMED, FAILED, Set.of(REFUNDED))
            .flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());

    private PaymentStatuses() {
    }

    public static String effective(Payment payment) {
        MercadoPagoOrder order = payment.providerResponse();
        if (order != null && order.getTransactions() != null && order.getTransactions().getPayments() != null
                && !order.getTransactions().getPayments().isEmpty()) {
            String status = order.getTransactions().getPayments().get(0).getStatus();
            if (status != null && !status.isBlank()) {
                return normalize(status);
            }
        }
        return normalize(payment.status());
    }

    public static boolean isConfirmed(String status) {
        return CONFIRMED.contains(normalize(status));
    }

    public static boolean isCanceled(String status) {
        return CANCELED.equals(normalize(status)) || "cancelled".equals(normalize(status));
    }

    public static boolean isRefunded(String status) {
        return REFUNDED.equals(normalize(status));
    }

    private static String normalize(String status) {
        return status == null ? "" : status.toLowerCase(Locale.ROOT);
    }
}
