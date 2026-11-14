package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.entity.PaymentJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentJpaEntity> findFirstByExternalReferenceOrderByCreatedAtDesc(String externalReference);

    @Query(value = """
            SELECT * FROM payments
            WHERE LOWER(status) NOT IN (:finalStatuses)
              AND mercado_pago_order_id IS NOT NULL
              AND updated_at < :updatedBefore
            ORDER BY updated_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentJpaEntity> findPendingForVerification(@Param("finalStatuses") Collection<String> finalStatuses,
                                                      @Param("updatedBefore") Instant updatedBefore,
                                                      @Param("limit") int limit);
}
