package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.messaging;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.event.RefundPaymentIntegrationCommand;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.RefundMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.RefundMercadoPagoPaymentCommand;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.RejeitarFinanceCommandUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.usecase.command.RejeitarFinanceCommandCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RefundPaymentIntegrationListenerTest {

    @Mock
    private RefundMercadoPagoPaymentUseCase useCase;
    @Mock
    private RejeitarFinanceCommandUseCase rejeitarFinanceCommandUseCase;
    @InjectMocks
    private RefundPaymentIntegrationListener listener;

    @Test
    @DisplayName("Deve executar o caso de uso com a OS do command")
    void shouldExecuteUseCase() {
        UUID workOrderId = UUID.randomUUID();

        listener.onMessage(new RefundPaymentIntegrationCommand(workOrderId));

        verify(useCase).execute(new RefundMercadoPagoPaymentCommand(workOrderId));
        verifyNoInteractions(rejeitarFinanceCommandUseCase);
    }

    @Test
    @DisplayName("Deve publicar a recusa e propagar a excecao quando o caso de uso falhar")
    void shouldRejectAndRethrowWhenUseCaseFails() {
        UUID workOrderId = UUID.randomUUID();
        PaymentException.PaymentNotFound failure = new PaymentException.PaymentNotFound();
        doThrow(failure).when(useCase).execute(new RefundMercadoPagoPaymentCommand(workOrderId));

        assertThatThrownBy(() -> listener.onMessage(new RefundPaymentIntegrationCommand(workOrderId))).isSameAs(failure);

        verify(rejeitarFinanceCommandUseCase).execute(new RejeitarFinanceCommandCommand(
                workOrderId, RefundPaymentIntegrationCommand.class.getSimpleName(), failure));
    }
}
