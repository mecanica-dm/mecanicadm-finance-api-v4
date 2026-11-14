package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command;

import java.util.UUID;

public record CancelMercadoPagoPaymentCommand(UUID workOrderId) {
}
