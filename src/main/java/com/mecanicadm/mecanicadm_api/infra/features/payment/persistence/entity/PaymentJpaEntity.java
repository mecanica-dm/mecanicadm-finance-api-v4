package com.mecanicadm.mecanicadm_api.infra.features.payment.persistence.entity;

import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_reference", nullable = false, length = 64)
    private String externalReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "mercado_pago_order_id", length = 64)
    private String mercadoPagoOrderId;

    @Column(name = "mercado_pago_payment_id", length = 64)
    private String mercadoPagoPaymentId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "status_detail", length = 128)
    private String statusDetail;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "installments", nullable = false)
    private Integer installments;

    @Column(name = "payment_method_id", length = 32)
    private String paymentMethodId;

    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private MercadoPagoOrder providerResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentJpaEntity() {
    }

    public PaymentJpaEntity(UUID id,
                            String externalReference,
                            String idempotencyKey,
                            String mercadoPagoOrderId,
                            String mercadoPagoPaymentId,
                            String status,
                            String statusDetail,
                            BigDecimal amount,
                            Integer installments,
                            String paymentMethodId,
                            String payerEmail,
                            MercadoPagoOrder providerResponse,
                            Instant createdAt,
                            Instant updatedAt) {
        this.id = id;
        this.externalReference = externalReference;
        this.idempotencyKey = idempotencyKey;
        this.mercadoPagoOrderId = mercadoPagoOrderId;
        this.mercadoPagoPaymentId = mercadoPagoPaymentId;
        this.status = status;
        this.statusDetail = statusDetail;
        this.amount = amount;
        this.installments = installments;
        this.paymentMethodId = paymentMethodId;
        this.payerEmail = payerEmail;
        this.providerResponse = providerResponse;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getMercadoPagoOrderId() {
        return mercadoPagoOrderId;
    }

    public String getMercadoPagoPaymentId() {
        return mercadoPagoPaymentId;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getInstallments() {
        return installments;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public MercadoPagoOrder getProviderResponse() {
        return providerResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
