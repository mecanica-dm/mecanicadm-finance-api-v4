package com.mecanicadm.mecanicadm_api.infra.features.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mercado-pago")
public class MercadoPagoProperties {

    private String accessToken;
    private String publicKey;
    private String clientId;
    private String clientSecret;
    private DefaultCard defaultCard = new DefaultCard();

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public DefaultCard getDefaultCard() {
        return defaultCard;
    }

    public void setDefaultCard(DefaultCard defaultCard) {
        this.defaultCard = defaultCard;
    }

    public static class DefaultCard {

        private String number = "4235647728025682";
        private String expirationMonth = "11";
        private String expirationYear = "2030";
        private String securityCode = "123";
        private String holderName = "APRO";
        private String identificationType = "CPF";
        private String identificationNumber = "19119119100";

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getExpirationMonth() {
            return expirationMonth;
        }

        public void setExpirationMonth(String expirationMonth) {
            this.expirationMonth = expirationMonth;
        }

        public String getExpirationYear() {
            return expirationYear;
        }

        public void setExpirationYear(String expirationYear) {
            this.expirationYear = expirationYear;
        }

        public String getSecurityCode() {
            return securityCode;
        }

        public void setSecurityCode(String securityCode) {
            this.securityCode = securityCode;
        }

        public String getHolderName() {
            return holderName;
        }

        public void setHolderName(String holderName) {
            this.holderName = holderName;
        }

        public String getIdentificationType() {
            return identificationType;
        }

        public void setIdentificationType(String identificationType) {
            this.identificationType = identificationType;
        }

        public String getIdentificationNumber() {
            return identificationNumber;
        }

        public void setIdentificationNumber(String identificationNumber) {
            this.identificationNumber = identificationNumber;
        }
    }
}
