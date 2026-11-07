package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
