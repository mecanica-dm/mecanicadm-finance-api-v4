package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.jpa;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.persistence.entity.PaymentJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentPersistenceTest {

    private Payment payment() {
        return new Payment(UUID.randomUUID(), "ref", "key", "ORD1", "PAY1", "processed", "accredited", new BigDecimal("720.00"),
                2, "visa", "a@a.com", new MercadoPagoOrder(), Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    @DisplayName("Mapper deve ir do dominio para a entidade e voltar sem perder campos")
    void shouldRoundTripBetweenDomainAndEntity() {
        Payment payment = payment();

        PaymentJpaEntity entity = PaymentJpaMapper.toEntity(payment);
        Payment back = PaymentJpaMapper.toDomain(entity);

        assertThat(back).usingRecursiveComparison().isEqualTo(payment);
        assertThat(entity.getStatus()).isEqualTo("processed");
        assertThat(PaymentJpaMapper.toEntity(null)).isNull();
        assertThat(PaymentJpaMapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("Gateway deve salvar e buscar por id, chave de idempotencia e referencia externa")
    void shouldSaveAndFind() {
        PaymentJpaRepository repository = mock(PaymentJpaRepository.class);
        PaymentGatewayImpl gateway = new PaymentGatewayImpl(repository);
        Payment payment = payment();
        PaymentJpaEntity entity = PaymentJpaMapper.toEntity(payment);
        when(repository.saveAndFlush(any())).thenReturn(entity);
        when(repository.findById(payment.id())).thenReturn(Optional.of(entity));
        when(repository.findByIdempotencyKey("key")).thenReturn(Optional.of(entity));
        when(repository.findFirstByExternalReferenceOrderByCreatedAtDesc("ref")).thenReturn(Optional.of(entity));

        assertThat(gateway.save(payment).id()).isEqualTo(payment.id());
        assertThat(gateway.findById(payment.id())).isPresent();
        assertThat(gateway.findByIdempotencyKey("key")).isPresent();
        assertThat(gateway.findLatestByExternalReference("ref")).isPresent();
        assertThat(gateway.findLatestByExternalReference("outra")).isEmpty();
    }

    @Test
    @DisplayName("Gateway deve devolver o pagamento ja existente quando a chave de idempotencia colidir")
    void shouldReturnExistingPaymentOnIdempotencyConflict() {
        PaymentJpaRepository repository = mock(PaymentJpaRepository.class);
        PaymentGatewayImpl gateway = new PaymentGatewayImpl(repository);
        Payment payment = payment();
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        when(repository.findByIdempotencyKey("key")).thenReturn(Optional.of(PaymentJpaMapper.toEntity(payment)));

        assertThat(gateway.save(payment).id()).isEqualTo(payment.id());

        when(repository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gateway.save(payment)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
