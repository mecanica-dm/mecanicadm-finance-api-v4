package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.GeneratePaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GeneratePaymentIntegrationListenerTest {

    @Mock
    private CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase;
    @Mock
    private RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;

    private GeneratePaymentIntegrationListener listener;

    @BeforeEach
    void setUp() {
        listener = new GeneratePaymentIntegrationListener(createMercadoPagoOrderUseCase,
                rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve executar o caso de uso com o id como idempotencyKey e externalReference")
    void shouldExecuteUseCaseWithIdAsIdempotencyKeyAndExternalReference() {
        String workOrderId = UUID.randomUUID().toString();
        GeneratePaymentIntegrationCommand command = new GeneratePaymentIntegrationCommand(workOrderId);

        listener.onMessage(command);

        verify(createMercadoPagoOrderUseCase).execute(new CreateMercadoPagoOrderCommand(
                workOrderId,
                workOrderId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
        verifyNoInteractions(rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve publicar evento de rejeição e propagar a exceção do serviço")
    void shouldPublishRejectionAndRethrowWhenServiceFails() {
        String workOrderId = UUID.randomUUID().toString();
        GeneratePaymentIntegrationCommand command = new GeneratePaymentIntegrationCommand(workOrderId);
        PaymentException.PaymentIdempotencyKeyInvalid failure =
                new PaymentException.PaymentIdempotencyKeyInvalid();
        doThrow(failure).when(createMercadoPagoOrderUseCase).execute(any(CreateMercadoPagoOrderCommand.class));

        assertThatThrownBy(() -> listener.onMessage(command))
                .isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                UUID.fromString(workOrderId), GeneratePaymentIntegrationCommand.class.getSimpleName(), failure));
    }

    @Test
    @DisplayName("Deve rejeitar com workOrderId nulo quando o id não é um UUID")
    void shouldRejectWithNullWorkOrderIdWhenIdIsNotUuid() {
        String id = "os-123";
        GeneratePaymentIntegrationCommand command = new GeneratePaymentIntegrationCommand(id);
        PaymentException.PaymentTotalAmountInvalid failure =
                new PaymentException.PaymentTotalAmountInvalid();
        doThrow(failure).when(createMercadoPagoOrderUseCase).execute(any(CreateMercadoPagoOrderCommand.class));

        assertThatThrownBy(() -> listener.onMessage(command))
                .isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                null, GeneratePaymentIntegrationCommand.class.getSimpleName(), failure));
    }
}
