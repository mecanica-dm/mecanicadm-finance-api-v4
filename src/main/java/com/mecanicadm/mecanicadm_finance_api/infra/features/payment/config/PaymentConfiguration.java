package com.mecanicadm.mecanicadm_finance_api.infra.features.payment.config;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoCredentials;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoAuthGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoCardTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.PaymentGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CancelMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.GenerateMercadoPagoTokenUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.GetMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.MercadoPagoAccessTokenProvider;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.PaymentOrderEventService;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.RefundMercadoPagoPaymentUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.payment.usecase.VerifyPendingPaymentsUseCase;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EventPublisher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MercadoPagoProperties.class, PaymentVerificationProperties.class})
public class PaymentConfiguration {

    @Bean
    public VerifyPendingPaymentsUseCase verifyPendingPaymentsUseCase(PaymentGateway paymentGateway,
                                                                     MercadoPagoOrderGateway orderGateway,
                                                                     MercadoPagoAccessTokenProvider accessTokenProvider,
                                                                     PaymentOrderEventService paymentOrderEventService) {
        return new VerifyPendingPaymentsUseCase(paymentGateway, orderGateway, accessTokenProvider, paymentOrderEventService);
    }

    @Bean
    public GenerateMercadoPagoTokenUseCase generateMercadoPagoTokenUseCase(MercadoPagoAuthGateway authGateway) {
        return new GenerateMercadoPagoTokenUseCase(authGateway);
    }

    @Bean
    public PaymentOrderEventService paymentOrderEventService(EventPublisher eventPublisher) {
        return new PaymentOrderEventService(eventPublisher);
    }

    @Bean
    public CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase(
            GenerateMercadoPagoTokenUseCase generateTokenUseCase,
            MercadoPagoCardTokenGateway cardTokenGateway,
            MercadoPagoOrderGateway orderGateway,
            PaymentGateway paymentGateway,
            MercadoPagoProperties properties,
            PaymentOrderEventService paymentOrderEventService) {

        MercadoPagoCredentials credentials = credentials(properties);

        MercadoPagoProperties.DefaultCard defaultCard = properties.getDefaultCard();
        CardData cardData = new CardData(
                defaultCard.getNumber(),
                defaultCard.getExpirationMonth(),
                defaultCard.getExpirationYear(),
                defaultCard.getSecurityCode(),
                defaultCard.getHolderName(),
                defaultCard.getIdentificationType(),
                defaultCard.getIdentificationNumber()
        );

        return new CreateMercadoPagoOrderUseCase(
                generateTokenUseCase,
                cardTokenGateway,
                orderGateway,
                paymentGateway,
                credentials,
                cardData,
                paymentOrderEventService
        );
    }

    private static MercadoPagoCredentials credentials(MercadoPagoProperties properties) {
        return new MercadoPagoCredentials(
                properties.getAccessToken(),
                properties.getPublicKey(),
                properties.getClientId(),
                properties.getClientSecret()
        );
    }

    @Bean
    public MercadoPagoAccessTokenProvider mercadoPagoAccessTokenProvider(GenerateMercadoPagoTokenUseCase generateTokenUseCase,
                                                                         MercadoPagoProperties properties) {
        return new MercadoPagoAccessTokenProvider(generateTokenUseCase, credentials(properties));
    }

    @Bean
    public CancelMercadoPagoPaymentUseCase cancelMercadoPagoPaymentUseCase(PaymentGateway paymentGateway,
                                                                           MercadoPagoOrderGateway orderGateway,
                                                                           MercadoPagoAccessTokenProvider accessTokenProvider,
                                                                           EventPublisher eventPublisher) {
        return new CancelMercadoPagoPaymentUseCase(paymentGateway, orderGateway, accessTokenProvider, eventPublisher);
    }

    @Bean
    public RefundMercadoPagoPaymentUseCase refundMercadoPagoPaymentUseCase(PaymentGateway paymentGateway,
                                                                           MercadoPagoOrderGateway orderGateway,
                                                                           MercadoPagoAccessTokenProvider accessTokenProvider,
                                                                           EventPublisher eventPublisher) {
        return new RefundMercadoPagoPaymentUseCase(paymentGateway, orderGateway, accessTokenProvider, eventPublisher);
    }

    @Bean
    public GetMercadoPagoPaymentUseCase getMercadoPagoPaymentUseCase(PaymentGateway paymentGateway) {
        return new GetMercadoPagoPaymentUseCase(paymentGateway);
    }
}
