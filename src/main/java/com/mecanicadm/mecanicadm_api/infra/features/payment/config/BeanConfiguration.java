package com.mecanicadm.mecanicadm_api.infra.features.payment.config;

import com.mecanicadm.mecanicadm_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_api.core.payment.domain.MercadoPagoCredentials;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoAuthGateway;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoCardTokenGateway;
import com.mecanicadm.mecanicadm_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.CreateMercadoPagoOrderUseCase;
import com.mecanicadm.mecanicadm_api.core.payment.usecase.GenerateMercadoPagoTokenUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class BeanConfiguration {

    @Bean
    public GenerateMercadoPagoTokenUseCase generateMercadoPagoTokenUseCase(MercadoPagoAuthGateway authGateway) {
        return new GenerateMercadoPagoTokenUseCase(authGateway);
    }

    @Bean
    public CreateMercadoPagoOrderUseCase createMercadoPagoOrderUseCase(
            GenerateMercadoPagoTokenUseCase generateTokenUseCase,
            MercadoPagoCardTokenGateway cardTokenGateway,
            MercadoPagoOrderGateway orderGateway,
            MercadoPagoProperties properties) {

        MercadoPagoCredentials credentials = new MercadoPagoCredentials(
                properties.getAccessToken(),
                properties.getPublicKey(),
                properties.getClientId(),
                properties.getClientSecret()
        );

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
                credentials,
                cardData
        );
    }
}
