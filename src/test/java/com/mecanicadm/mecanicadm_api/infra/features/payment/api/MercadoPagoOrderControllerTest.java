package com.mecanicadm.mecanicadm_api.infra.features.payment.api;

import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_api.infra.features.payment.api.request.CreateOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoOrderControllerTest {

    @Mock
    private CreateMercadoPagoOrderUseCase createOrderUseCase;

    @InjectMocks
    private MercadoPagoOrderController controller;

    @Test
    @DisplayName("Deve criar pedido e retornar 201 usando a chave de idempotência informada")
    void shouldCreateOrderAndReturn201UsingProvidedIdempotencyKey() {
        MercadoPagoOrder order = new MercadoPagoOrder();
        when(createOrderUseCase.execute(any())).thenReturn(order);

        var response = controller.createOrder("idempotency-key", request());

        assertEquals(201, response.getStatusCode().value());
        assertSame(order, response.getBody());

        ArgumentCaptor<CreateMercadoPagoOrderCommand> captor =
                ArgumentCaptor.forClass(CreateMercadoPagoOrderCommand.class);
        verify(createOrderUseCase).execute(captor.capture());
        assertEquals("idempotency-key", captor.getValue().idempotencyKey());
    }

    @Test
    @DisplayName("Deve gerar uma chave de idempotência quando o header não for informado")
    void shouldGenerateIdempotencyKeyWhenHeaderIsMissing() {
        when(createOrderUseCase.execute(any())).thenReturn(new MercadoPagoOrder());

        var response = controller.createOrder(null, request());

        assertEquals(201, response.getStatusCode().value());

        ArgumentCaptor<CreateMercadoPagoOrderCommand> captor =
                ArgumentCaptor.forClass(CreateMercadoPagoOrderCommand.class);
        verify(createOrderUseCase).execute(captor.capture());
        assertNotNull(captor.getValue().idempotencyKey());
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(
                "external-reference",
                "Serviço de mecânica",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
