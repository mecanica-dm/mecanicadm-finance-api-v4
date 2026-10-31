package com.mecanicadm.mecanicadm_api.infra.features.workorder.persistence.jpa;

import com.mecanicadm.mecanicadm_api.core.workorder.domain.WorkOrderBudget;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.port.WorkOrderBudgetGateway;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class WorkOrderBudgetGatewayImpl implements WorkOrderBudgetGateway {

    private final WorkOrderBudgetJpaRepository jpaRepository;

    public WorkOrderBudgetGatewayImpl(WorkOrderBudgetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(WorkOrderBudget budget) {
        jpaRepository.save(WorkOrderBudgetJpaMapper.toEntity(budget));
    }

    @Override
    public Optional<WorkOrderBudget> findById(UUID workOrderId) {
        return jpaRepository.findById(workOrderId).map(WorkOrderBudgetJpaMapper::toDomain);
    }
}
