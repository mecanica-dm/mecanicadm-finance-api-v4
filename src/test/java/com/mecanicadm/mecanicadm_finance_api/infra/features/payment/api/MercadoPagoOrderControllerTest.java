package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.GetMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.CreateOrderRequest;
import com.mecanicadm.mecanicadm_finance_api.infra.features.payment.api.request.SimulateOrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoOrderControllerTest {

    @Mock
    private CreateMercadoPagoOrderUseCase createOrderUseCase;

    @Mock
    private GetMercadoPagoPaymentUseCase getPaymentUseCase;

    @InjectMocks
    private MercadoPagoOrderController controller;

    private UUID paymentId;

    @BeforeEach
    void setUpRequestContext() {
        paymentId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/payments/orders");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDownRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Deve criar pedido e retornar 201 usando a chave de idempotência informada")
    void shouldCreateOrderAndReturn201UsingProvidedIdempotencyKey() {
        MercadoPagoOrder order = new MercadoPagoOrder();
        when(createOrderUseCase.execute(any())).thenReturn(payment(order));

        var response = controller.createOrder("idempotency-key", request());

        assertEquals(201, response.getStatusCode().value());
        assertSame(order, response.getBody());
        assertEquals("/v1/payments/orders/" + paymentId, response.getHeaders().getLocation().getPath());

        ArgumentCaptor<CreateMercadoPagoOrderCommand> captor =
                ArgumentCaptor.forClass(CreateMercadoPagoOrderCommand.class);
        verify(createOrderUseCase).execute(captor.capture());
        assertEquals("idempotency-key", captor.getValue().idempotencyKey());
    }

    @Test
    @DisplayName("Deve gerar uma chave de idempotência quando o header não for informado")
    void shouldGenerateIdempotencyKeyWhenHeaderIsMissing() {
        when(createOrderUseCase.execute(any())).thenReturn(payment(new MercadoPagoOrder()));

        var response = controller.createOrder(null, request());

        assertEquals(201, response.getStatusCode().value());

        ArgumentCaptor<CreateMercadoPagoOrderCommand> captor =
                ArgumentCaptor.forClass(CreateMercadoPagoOrderCommand.class);
        verify(createOrderUseCase).execute(captor.capture());
        assertNotNull(captor.getValue().idempotencyKey());
    }

    @Test
    @DisplayName("Deve simular pedido usando apenas a referência externa e retornar 201")
    void shouldSimulateOrderUsingOnlyExternalReference() {
        MercadoPagoOrder order = new MercadoPagoOrder();
        when(createOrderUseCase.execute(any())).thenReturn(payment(order));

        var response = controller.simulate(new SimulateOrderRequest("work-order-id"));

        assertEquals(201, response.getStatusCode().value());
        assertSame(order, response.getBody());
        assertEquals("/v1/payments/orders/" + paymentId, response.getHeaders().getLocation().getPath());

        ArgumentCaptor<CreateMercadoPagoOrderCommand> captor =
                ArgumentCaptor.forClass(CreateMercadoPagoOrderCommand.class);
        verify(createOrderUseCase).execute(captor.capture());

        CreateMercadoPagoOrderCommand command = captor.getValue();
        assertEquals("work-order-id", command.externalReference());
        assertNotNull(command.idempotencyKey());
        assertNull(command.description());
        assertNull(command.totalAmount());
        assertNull(command.payerEmail());
        assertNull(command.installments());
        assertNull(command.paymentMethodId());
        assertNull(command.card());
    }

    @Test
    @DisplayName("Deve consultar o pedido persistido pelo identificador")
    void shouldFindStoredOrderById() {
        UUID id = UUID.randomUUID();
        MercadoPagoOrder order = new MercadoPagoOrder();
        when(getPaymentUseCase.execute(id)).thenReturn(payment(order));

        var response = controller.findById(id);

        assertEquals(200, response.getStatusCode().value());
        assertSame(order, response.getBody());
        verify(getPaymentUseCase).execute(id);
    }

    private Payment payment(MercadoPagoOrder order) {
        Instant now = Instant.now();
        return new Payment(
                paymentId,
                "external-reference",
                "idempotency-key",
                "mp-order-1",
                "mp-payment-1",
                "approved",
                null,
                new BigDecimal("50.00"),
                1,
                "visa",
                "test_payer@example.com",
                order,
                now,
                now
        );
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
