package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BudgetExpiryGateway {

    List<ExpiredBudget> claimExpired(LocalDateTime createdBefore, int limit);

    void markExpiryNotified(UUID tokenId);

    record ExpiredBudget(UUID tokenId, UUID workOrderId) {
    }
}
