package com.mecanicadm.mecanicadm_api.core.workorder.exception;

import com.mecanicadm.mecanicadm_api.shared.exception.DomainExceptionCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BudgetExceptionsTest {

    @Test
    @DisplayName("Deve cobrir o construtor privado de BudgetExceptions")
    void shouldCoverPrivateConstructor() throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<BudgetExceptions> constructor = BudgetExceptions.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        BudgetExceptions instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Deve instanciar todas as exceções de orçamento para garantir cobertura")
    void shouldInstantiateAllBudgetExceptions() {
        assertException(new BudgetExceptions.ClientRequired(), 400, "budget.client.required");
        assertException(new BudgetExceptions.VehicleRequired(), 400, "budget.vehicle.required");
        assertException(new BudgetExceptions.BudgetNotFound(), 404, "budget.not.found");
        assertException(new BudgetExceptions.BudgetDecisionInvalid("INVALID"), 400, "budget.decision.invalid");
        assertException(new BudgetExceptions.BudgetObservationRequired(), 400, "budget.observation.required");
        assertException(new BudgetExceptions.BudgetNotWaitingDecision(), 400, "budget.not.waiting.decision");
        assertException(new BudgetExceptions.BudgetTotalPriceInvalid(), 400, "budget.total.price.invalid");
        assertException(new BudgetExceptions.BudgetStatusInvalid(), 400, "budget.status.invalid");
        assertException(new BudgetExceptions.BudgetTokenNotFound(), 404, "budget.token.not.found");
        assertException(BudgetExceptions.BudgetTokenInvalid.of("expirado"), 400, "budget.token.invalid");
    }

    private void assertException(DomainExceptionCore exception, int expectedStatus, String expectedKey) {
        assertEquals(expectedStatus, exception.getStatus());
        assertEquals(expectedKey, exception.getMessageKey());
    }
}
