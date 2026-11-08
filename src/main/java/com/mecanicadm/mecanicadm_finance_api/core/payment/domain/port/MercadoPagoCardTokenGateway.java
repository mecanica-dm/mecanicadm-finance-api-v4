package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.CardData;

public interface MercadoPagoCardTokenGateway {
    String createCardToken(CardData cardData, String publicKey);
}
