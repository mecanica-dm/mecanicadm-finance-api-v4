package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command;

import java.time.Duration;

public record VerifyPendingPaymentsCommand(Duration minAge, int batchSize) {
}
