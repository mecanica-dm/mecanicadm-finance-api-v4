package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event;

import java.util.UUID;

public record RefundPaymentIntegrationCommand(UUID workOrderId) {
}
