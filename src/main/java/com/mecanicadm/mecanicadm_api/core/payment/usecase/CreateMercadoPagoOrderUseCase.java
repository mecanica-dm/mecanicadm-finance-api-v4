package com.mecanicadm.mecanicadm_api.core.payment.usecase;

import com.mecanicadm.mecanicadm_api.core.payment.domain.AuthToken;
import com.mecanicadm.mecanicadm_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoCredentials;
import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoOrderData;
import com.mecanicadm.mecanicadm_api.core.payment.domain.Payment;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoCardTokenGateway;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_api.core.payment.exception.PaymentException;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.command.CreateMercadoPagoOrderCommand;
import com.mecanicadm.mecanicadm_api.shared.usecase.UseCase;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CreateMercadoPagoOrderUseCase implements UseCase<CreateMercadoPagoOrderCommand, Payment> {

    private static final String DEFAULT_DESCRIPTION = "Pagamento Mecanica DM";
    private static final String DEFAULT_PAYER_EMAIL = "test_payer@example.com";
    private static final String DEFAULT_PAYER_FIRST_NAME = "Joao";
    private static final String DEFAULT_PAYER_LAST_NAME = "Silva";
    private static final BigDecimal DEFAULT_TOTAL_AMOUNT = new BigDecimal("50.00");
    private static final String DEFAULT_PAYMENT_METHOD_ID = "visa";
    private static final String DEFAULT_PAYMENT_METHOD_TYPE = "credit_card";
    private static final int DEFAULT_INSTALLMENTS = 1;
    private static final int MAX_INSTALLMENTS = 36;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;
    private static final int MAX_EXTERNAL_REFERENCE_LENGTH = 64;

    private final GenerateMercadoPagoTokenUseCase generateTokenUseCase;
    private final MercadoPagoCardTokenGateway cardTokenGateway;
    private final MercadoPagoOrderGateway orderGateway;
    private final PaymentGateway paymentGateway;
    private final MercadoPagoCredentials credentials;
    private final CardData defaultCard;

    public CreateMercadoPagoOrderUseCase(GenerateMercadoPagoTokenUseCase generateTokenUseCase,
                                         MercadoPagoCardTokenGateway cardTokenGateway,
                                         MercadoPagoOrderGateway orderGateway,
                                         PaymentGateway paymentGateway,
                                         MercadoPagoCredentials credentials,
                                         CardData defaultCard) {
        this.generateTokenUseCase = generateTokenUseCase;
        this.cardTokenGateway = cardTokenGateway;
        this.orderGateway = orderGateway;
        this.paymentGateway = paymentGateway;
        this.credentials = credentials;
        this.defaultCard = defaultCard;
    }

    @Override
    public Payment execute(CreateMercadoPagoOrderCommand command) {
        validateIdempotencyKey(command.idempotencyKey());

        Payment storedPayment = paymentGateway.findByIdempotencyKey(command.idempotencyKey()).orElse(null);
        if (storedPayment != null && storedPayment.providerResponse() != null) {
            return storedPayment;
        }

        String externalReference = resolveExternalReference(command);
        String description = valueOrDefault(command.description(), DEFAULT_DESCRIPTION);
        BigDecimal amount = resolveTotalAmount(command.totalAmount());
        String totalAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String payerEmail = valueOrDefault(command.payerEmail(), DEFAULT_PAYER_EMAIL);
        String payerFirstName = valueOrDefault(command.payerFirstName(), DEFAULT_PAYER_FIRST_NAME);
        String payerLastName = valueOrDefault(command.payerLastName(), DEFAULT_PAYER_LAST_NAME);
        String paymentMethodId = valueOrDefault(command.paymentMethodId(), DEFAULT_PAYMENT_METHOD_ID);
        String paymentMethodType = valueOrDefault(command.paymentMethodType(), DEFAULT_PAYMENT_METHOD_TYPE);
        int installments = resolveInstallments(command.installments());

        AuthToken authToken = resolveAuthToken();
        String publicKey = resolvePublicKey(authToken);
        CardData card = resolveCard(command.card());
        String cardToken = cardTokenGateway.createCardToken(card, publicKey);

        if (cardToken == null || cardToken.isBlank()) {
            throw new PaymentException.PaymentCardTokenNotGenerated();
        }

        MercadoPagoOrderData orderData = new MercadoPagoOrderData(
                externalReference,
                description,
                totalAmount,
                payerEmail,
                payerFirstName,
                payerLastName,
                paymentMethodId,
                paymentMethodType,
                cardToken,
                installments
        );

        MercadoPagoOrder order = orderGateway.createOrder(orderData, authToken.getAccessToken(), command.idempotencyKey());

        return paymentGateway.save(Payment.create(
                externalReference,
                command.idempotencyKey(),
                amount,
                installments,
                paymentMethodId,
                payerEmail,
                order
        ));
    }

    private AuthToken resolveAuthToken() {
        if (credentials.hasAccessToken()) {
            AuthToken authToken = new AuthToken();
            authToken.setAccessToken(credentials.accessToken());
            authToken.setPublicKey(credentials.publicKey());
            return authToken;
        }

        if (!credentials.hasClientCredentials()) {
            throw new PaymentException.PaymentAccessTokenRequired();
        }

        AuthToken authToken = generateTokenUseCase.execute(credentials.clientId(), credentials.clientSecret());
        if (authToken == null || authToken.getAccessToken() == null || authToken.getAccessToken().isBlank()) {
            throw new PaymentException.PaymentAccessTokenRequired();
        }

        return authToken;
    }

    private String resolvePublicKey(AuthToken authToken) {
        String publicKey = credentials.hasPublicKey() ? credentials.publicKey() : authToken.getPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            throw new PaymentException.PaymentPublicKeyRequired();
        }
        return publicKey;
    }

    private CardData resolveCard(CardData card) {
        CardData resolvedCard = card == null ? defaultCard : card.merge(defaultCard);
        if (resolvedCard == null || !resolvedCard.isComplete()) {
            throw new PaymentException.PaymentCardDataInvalid();
        }
        return resolvedCard;
    }

    private String resolveExternalReference(CreateMercadoPagoOrderCommand command) {
        String externalReference = valueOrDefault(command.externalReference(), command.idempotencyKey());
        String sanitized = externalReference.replaceAll("[^A-Za-z0-9_-]", "_");
        return sanitized.length() > MAX_EXTERNAL_REFERENCE_LENGTH
                ? sanitized.substring(0, MAX_EXTERNAL_REFERENCE_LENGTH)
                : sanitized;
    }

    private BigDecimal resolveTotalAmount(BigDecimal totalAmount) {
        BigDecimal amount = totalAmount == null ? DEFAULT_TOTAL_AMOUNT : totalAmount;
        if (amount.signum() <= 0) {
            throw new PaymentException.PaymentTotalAmountInvalid();
        }
        return amount;
    }

    private int resolveInstallments(Integer installments) {
        int resolvedInstallments = installments == null ? DEFAULT_INSTALLMENTS : installments;
        if (resolvedInstallments < 1 || resolvedInstallments > MAX_INSTALLMENTS) {
            throw new PaymentException.PaymentInstallmentsInvalid();
        }
        return resolvedInstallments;
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new PaymentException.PaymentIdempotencyKeyInvalid();
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
