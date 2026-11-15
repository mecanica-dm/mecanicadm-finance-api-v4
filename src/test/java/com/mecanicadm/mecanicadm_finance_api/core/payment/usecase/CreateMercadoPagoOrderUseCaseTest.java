package com.mecanicadm.mecanicadm_finance_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoCredentials;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrderData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoCardTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMercadoPagoOrderUseCaseTest {

    private static final String ACCESS_TOKEN = "app-user-access-token";
    private static final String PUBLIC_KEY = "app-user-public-key";
    private static final String CARD_TOKEN = "card-token-generated";

    @Mock
    private GenerateMercadoPagoTokenUseCase generateTokenUseCase;
    @Mock
    private MercadoPagoCardTokenGateway cardTokenGateway;
    @Mock
    private MercadoPagoOrderGateway orderGateway;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentOrderEventService paymentOrderEventService;

    private CardData defaultCard;

    @BeforeEach
    void setUp() {
        defaultCard = new CardData(
                "4235647728025682",
                "11",
                "2030",
                "123",
                "APRO",
                "CPF",
                "19119119100"
        );
    }

    @Test
    @DisplayName("Deve criar pedido com dados padrão do cartão e access token configurado")
    void shouldCreateOrderUsingDefaultCardAndStaticAccessToken() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));
        String idempotencyKey = UUID.randomUUID().toString();
        MercadoPagoOrder expectedOrder = new MercadoPagoOrder();

        when(cardTokenGateway.createCardToken(defaultCard, PUBLIC_KEY)).thenReturn(CARD_TOKEN);
        when(orderGateway.createOrder(any(MercadoPagoOrderData.class), eq(ACCESS_TOKEN), anyString()))
                .thenReturn(expectedOrder);
        when(paymentGateway.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = useCase.execute(command(idempotencyKey, null));

        assertSame(expectedOrder, result.providerResponse());
        verify(generateTokenUseCase, never()).execute(anyString(), anyString());

        ArgumentCaptor<MercadoPagoOrderData> captor = ArgumentCaptor.forClass(MercadoPagoOrderData.class);
        verify(orderGateway).createOrder(captor.capture(), eq(ACCESS_TOKEN), anyString());

        MercadoPagoOrderData orderData = captor.getValue();
        assertEquals(idempotencyKey, orderData.externalReference());
        assertEquals("Pagamento Mecanica DM", orderData.description());
        assertEquals("50.00", orderData.totalAmount());
        assertEquals("test_payer@example.com", orderData.payerEmail());
        assertEquals("Joao", orderData.payerFirstName());
        assertEquals("Silva", orderData.payerLastName());
        assertEquals("visa", orderData.paymentMethodId());
        assertEquals("credit_card", orderData.paymentMethodType());
        assertEquals(CARD_TOKEN, orderData.cardToken());
        assertEquals(1, orderData.installments());
    }

    @Test
    @DisplayName("Deve completar os dados do cartão informado com os valores padrão")
    void shouldMergeProvidedCardWithDefaultValues() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));
        String idempotencyKey = UUID.randomUUID().toString();
        CardData providedCard = new CardData("4111111111111111", null, null, null, null, null, null);
        CardData expectedCard = new CardData(
                "4111111111111111", "11", "2030", "123", "APRO", "CPF", "19119119100");

        when(cardTokenGateway.createCardToken(expectedCard, PUBLIC_KEY)).thenReturn(CARD_TOKEN);
        when(orderGateway.createOrder(any(MercadoPagoOrderData.class), eq(ACCESS_TOKEN), anyString()))
                .thenReturn(new MercadoPagoOrder());

        useCase.execute(command(idempotencyKey, providedCard));

        verify(cardTokenGateway).createCardToken(expectedCard, PUBLIC_KEY);
    }

    @Test
    @DisplayName("Deve gerar o token do Mercado Pago quando o access token não estiver configurado")
    void shouldGenerateTokenWhenAccessTokenIsNotConfigured() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                null, null, "client-id", "client-secret"));
        String idempotencyKey = UUID.randomUUID().toString();
        AuthToken authToken = new AuthToken();
        authToken.setAccessToken(ACCESS_TOKEN);
        authToken.setPublicKey(PUBLIC_KEY);

        when(generateTokenUseCase.execute("client-id", "client-secret")).thenReturn(authToken);
        when(cardTokenGateway.createCardToken(defaultCard, PUBLIC_KEY)).thenReturn(CARD_TOKEN);
        when(orderGateway.createOrder(any(MercadoPagoOrderData.class), eq(ACCESS_TOKEN), anyString()))
                .thenReturn(new MercadoPagoOrder());

        useCase.execute(command(idempotencyKey, null));

        verify(generateTokenUseCase).execute("client-id", "client-secret");
        verify(orderGateway).createOrder(any(MercadoPagoOrderData.class), eq(ACCESS_TOKEN), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando nenhuma credencial estiver configurada")
    void shouldThrowWhenCredentialsAreNotConfigured() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(null, null, null, null));

        assertThrows(PaymentException.PaymentAccessTokenRequired.class,
                () -> useCase.execute(command(UUID.randomUUID().toString(), null)));

        verifyNoInteractions(cardTokenGateway, orderGateway);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a chave pública não estiver configurada")
    void shouldThrowWhenPublicKeyIsNotConfigured() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                null, null, "client-id", "client-secret"));
        AuthToken authToken = new AuthToken();
        authToken.setAccessToken(ACCESS_TOKEN);

        when(generateTokenUseCase.execute("client-id", "client-secret")).thenReturn(authToken);

        assertThrows(PaymentException.PaymentPublicKeyRequired.class,
                () -> useCase.execute(command(UUID.randomUUID().toString(), null)));

        verifyNoInteractions(cardTokenGateway, orderGateway);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a chave de idempotência for inválida")
    void shouldThrowWhenIdempotencyKeyIsInvalid() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));

        assertThrows(PaymentException.PaymentIdempotencyKeyInvalid.class,
                () -> useCase.execute(command("x".repeat(129), null)));

        verifyNoInteractions(cardTokenGateway, orderGateway);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o valor total for inválido")
    void shouldThrowWhenTotalAmountIsInvalid() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));
        String idempotencyKey = UUID.randomUUID().toString();
        CreateMercadoPagoOrderCommand command = new CreateMercadoPagoOrderCommand(
                idempotencyKey, null, null, BigDecimal.ZERO, null, null, null, null, null, null, null);

        assertThrows(PaymentException.PaymentTotalAmountInvalid.class,
                () -> useCase.execute(command));

        verifyNoInteractions(cardTokenGateway, orderGateway);
    }

    @Test
    @DisplayName("Deve persistir o pagamento após criar o pedido no Mercado Pago")
    void shouldPersistPaymentAfterCreatingOrder() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));
        String idempotencyKey = UUID.randomUUID().toString();
        MercadoPagoOrder expectedOrder = order("mp-order-1", "approved", "mp-payment-1");

        when(cardTokenGateway.createCardToken(defaultCard, PUBLIC_KEY)).thenReturn(CARD_TOKEN);
        when(orderGateway.createOrder(any(MercadoPagoOrderData.class), eq(ACCESS_TOKEN), anyString()))
                .thenReturn(expectedOrder);
        when(paymentGateway.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = useCase.execute(command(idempotencyKey, null));

        assertSame(expectedOrder, result.providerResponse());

        UUID workOrderId = UUID.fromString(idempotencyKey);
        verify(paymentOrderEventService).publishLinkGenerated(any(Payment.class), eq(workOrderId));
        verify(paymentOrderEventService).publishOutcome(any(Payment.class), eq(workOrderId));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentGateway).save(captor.capture());

        Payment payment = captor.getValue();
        assertEquals(idempotencyKey, payment.externalReference());
        assertEquals(idempotencyKey, payment.idempotencyKey());
        assertEquals("mp-order-1", payment.mercadoPagoOrderId());
        assertEquals("mp-payment-1", payment.mercadoPagoPaymentId());
        assertEquals("approved", payment.status());
        assertEquals(new BigDecimal("50.00"), payment.amount());
        assertEquals(1, payment.installments());
        assertEquals("visa", payment.paymentMethodId());
        assertEquals("test_payer@example.com", payment.payerEmail());
        assertSame(expectedOrder, payment.providerResponse());
    }

    @Test
    @DisplayName("Deve publicar falha na geração do link quando o Mercado Pago retornar erro")
    void shouldPublishLinkGenerationFailedWhenProviderFails() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));
        String idempotencyKey = UUID.randomUUID().toString();
        UUID workOrderId = UUID.randomUUID();
        CreateMercadoPagoOrderCommand command = new CreateMercadoPagoOrderCommand(
                idempotencyKey, workOrderId.toString(), null, null, null,
                null, null, null, null, null, null);

        when(cardTokenGateway.createCardToken(defaultCard, PUBLIC_KEY)).thenReturn(CARD_TOKEN);
        when(orderGateway.createOrder(any(MercadoPagoOrderData.class), eq(ACCESS_TOKEN), anyString()))
                .thenThrow(new PaymentException.MercadoPagoProviderError(502, "provider unavailable"));

        assertThrows(PaymentException.MercadoPagoProviderError.class, () -> useCase.execute(command));

        verify(paymentOrderEventService).publishLinkGenerationFailed(
                workOrderId, workOrderId.toString(), "provider unavailable");
        verify(paymentOrderEventService, never()).publishLinkGenerated(any(), any());
        verify(paymentOrderEventService, never()).publishOutcome(any(), any());
        verify(paymentGateway, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar o pedido persistido quando a chave de idempotência já existir")
    void shouldReturnStoredOrderWhenIdempotencyKeyAlreadyExists() {
        CreateMercadoPagoOrderUseCase useCase = useCaseWith(new MercadoPagoCredentials(
                ACCESS_TOKEN, PUBLIC_KEY, null, null));
        String idempotencyKey = UUID.randomUUID().toString();
        MercadoPagoOrder storedOrder = order("mp-order-1", "approved", "mp-payment-1");
        Instant now = Instant.now();
        Payment storedPayment = new Payment(
                UUID.randomUUID(),
                "external-reference",
                idempotencyKey,
                "mp-order-1",
                "mp-payment-1",
                "approved",
                null,
                new BigDecimal("50.00"),
                1,
                "visa",
                "test_payer@example.com",
                storedOrder,
                now,
                now
        );

        when(paymentGateway.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(storedPayment));

        Payment result = useCase.execute(command(idempotencyKey, null));

        assertSame(storedPayment, result);
        verifyNoInteractions(generateTokenUseCase, cardTokenGateway, orderGateway);
        verify(paymentGateway, never()).save(any());
    }

    private MercadoPagoOrder order(String id, String status, String paymentId) {
        MercadoPagoOrder.PaymentMethod paymentMethod = new MercadoPagoOrder.PaymentMethod();
        paymentMethod.setId("visa");

        MercadoPagoOrder.Payment payment = new MercadoPagoOrder.Payment();
        payment.setId(paymentId);
        payment.setPaymentMethod(paymentMethod);

        MercadoPagoOrder.Transactions transactions = new MercadoPagoOrder.Transactions();
        transactions.setPayments(List.of(payment));

        MercadoPagoOrder order = new MercadoPagoOrder();
        order.setId(id);
        order.setStatus(status);
        order.setTransactions(transactions);
        return order;
    }

    private CreateMercadoPagoOrderUseCase useCaseWith(MercadoPagoCredentials credentials) {
        return new CreateMercadoPagoOrderUseCase(
                generateTokenUseCase,
                cardTokenGateway,
                orderGateway,
                paymentGateway,
                credentials,
                defaultCard,
                paymentOrderEventService
        );
    }

    private CreateMercadoPagoOrderCommand command(String idempotencyKey, CardData card) {
        return new CreateMercadoPagoOrderCommand(
                idempotencyKey,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                card
        );
    }
}
