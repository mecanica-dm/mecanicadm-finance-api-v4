package com.mecanicadm.mecanicadm_api.core.workorder.exception;

import com.mecanicadm.mecanicadm_api.shared.exception.DomainExceptionCore;

public class BudgetExceptions {

    private BudgetExceptions() {
    }

    public static class ClientRequired extends DomainExceptionCore {
        public ClientRequired() {
            super("budget.client.required", 400);
        }
    }

    public static class VehicleRequired extends DomainExceptionCore {
        public VehicleRequired() {
            super("budget.vehicle.required", 400);
        }
    }

    public static class BudgetNotFound extends DomainExceptionCore {
        public BudgetNotFound() {
            super("budget.not.found", 404);
        }
    }

    public static class BudgetDecisionInvalid extends DomainExceptionCore {
        public BudgetDecisionInvalid(String decision) {
            super("budget.decision.invalid", 400, decision);
        }
    }

    public static class BudgetObservationRequired extends DomainExceptionCore {
        public BudgetObservationRequired() {
            super("budget.observation.required", 400);
        }
    }

    public static class BudgetNotWaitingDecision extends DomainExceptionCore {
        public BudgetNotWaitingDecision() {
            super("budget.not.waiting.decision", 400);
        }
    }

    public static class BudgetTotalPriceInvalid extends DomainExceptionCore {
        public BudgetTotalPriceInvalid() {
            super("budget.total.price.invalid", 400);
        }
    }

    public static class BudgetStatusInvalid extends DomainExceptionCore {
        public BudgetStatusInvalid() {
            super("budget.status.invalid", 400);
        }
    }

    public static class BudgetTokenNotFound extends DomainExceptionCore {
        public BudgetTokenNotFound() {
            super("budget.token.not.found", 404);
        }
    }

    public static class BudgetTokenInvalid extends DomainExceptionCore {
        private BudgetTokenInvalid(String detail) {
            super("budget.token.invalid", 400, detail);
        }

        public static BudgetTokenInvalid of(String detail) {
            return new BudgetTokenInvalid(detail);
        }
    }
}
