package com.mecanicadm.mecanicadm_finance_api.core.payment.exception;

import com.mecanicadm.mecanicadm_finance_api.shared.exception.DomainExceptionCore;

public class PaymentException {

    private PaymentException() {
    }

    public static class PaymentGrantTypeRequired extends DomainExceptionCore {
        public PaymentGrantTypeRequired() {
            super("payment.grant.type.required", 400);
        }
    }

    public static class PaymentAccessTokenRequired extends DomainExceptionCore {
        public PaymentAccessTokenRequired() {
            super("payment.access.token.required", 400);
        }
    }

    public static class PaymentPublicKeyRequired extends DomainExceptionCore {
        public PaymentPublicKeyRequired() {
            super("payment.public.key.required", 400);
        }
    }

    public static class PaymentCardDataInvalid extends DomainExceptionCore {
        public PaymentCardDataInvalid() {
            super("payment.card.data.invalid", 400);
        }
    }

    public static class PaymentCardTokenNotGenerated extends DomainExceptionCore {
        public PaymentCardTokenNotGenerated() {
            super("payment.card.token.not.generated", 502);
        }
    }

    public static class PaymentIdempotencyKeyInvalid extends DomainExceptionCore {
        public PaymentIdempotencyKeyInvalid() {
            super("payment.idempotency.key.invalid", 400);
        }
    }

    public static class PaymentTotalAmountInvalid extends DomainExceptionCore {
        public PaymentTotalAmountInvalid() {
            super("payment.total.amount.invalid", 400);
        }
    }

    public static class PaymentInstallmentsInvalid extends DomainExceptionCore {
        public PaymentInstallmentsInvalid() {
            super("payment.installments.invalid", 400);
        }
    }

    public static class MercadoPagoProviderError extends DomainExceptionCore {
        public MercadoPagoProviderError(int status, String detail) {
            super("payment.provider.error", status, detail);
        }
    }

    public static class PaymentNotFound extends DomainExceptionCore {
        public PaymentNotFound() {
            super("payment.not.found", 404);
        }
    }
}
