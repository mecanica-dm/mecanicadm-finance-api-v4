package com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrderData;

public interface MercadoPagoOrderGateway {
    MercadoPagoOrder createOrder(MercadoPagoOrderData orderData, String accessToken, String idempotencyKey);

    MercadoPagoOrder getOrder(String orderId, String accessToken);

    MercadoPagoOrder cancelOrder(String orderId, String accessToken, String idempotencyKey);

    MercadoPagoOrder refundOrder(String orderId, String accessToken, String idempotencyKey);
}
