package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.PaymentStatuses;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.entity.PaymentJpaEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentGatewayImpl implements PaymentGateway {

    private final PaymentJpaRepository jpaRepository;

    public PaymentGatewayImpl(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        try {
            PaymentJpaEntity saved = jpaRepository.saveAndFlush(PaymentJpaMapper.toEntity(payment));
            return PaymentJpaMapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            return jpaRepository.findByIdempotencyKey(payment.idempotencyKey())
                    .map(PaymentJpaMapper::toDomain)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(PaymentJpaMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(PaymentJpaMapper::toDomain);
    }

    @Override
    public List<Payment> findPendingForVerification(Instant updatedBefore, int limit) {
        return jpaRepository.findPendingForVerification(PaymentStatuses.FINAL, updatedBefore, limit).stream()
                .map(PaymentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Payment> findLatestByExternalReference(String externalReference) {
        return jpaRepository.findFirstByExternalReferenceOrderByCreatedAtDesc(externalReference).map(PaymentJpaMapper::toDomain);
    }
}
