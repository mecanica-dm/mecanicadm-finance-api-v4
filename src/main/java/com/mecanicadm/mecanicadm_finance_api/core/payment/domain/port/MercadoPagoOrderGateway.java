package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrderData;

public interface MercadoPagoOrderGateway {
    MercadoPagoOrder createOrder(MercadoPagoOrderData orderData, String accessToken, String idempotencyKey);
}
