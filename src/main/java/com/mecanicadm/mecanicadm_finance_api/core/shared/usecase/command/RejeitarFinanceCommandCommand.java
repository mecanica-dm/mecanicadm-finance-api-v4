package com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command;

import java.util.UUID;

public record RejeitarFinanceCommandCommand(
        UUID workOrderId,
        String eventType,
        RuntimeException exception
) {
}
