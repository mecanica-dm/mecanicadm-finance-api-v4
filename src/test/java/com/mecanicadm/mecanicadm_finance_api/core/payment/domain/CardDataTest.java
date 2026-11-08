package com.mecanicadm.mecanicadm_finance_api.core.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardDataTest {

    private final CardData defaultCard = new CardData(
            "4235647728025682", "11", "2030", "123", "APRO", "CPF", "19119119100");

    @Test
    @DisplayName("Deve manter os dados informados e completar apenas os campos nulos")
    void shouldMergeOnlyMissingFields() {
        CardData providedCard = new CardData("4111111111111111", null, "2028", " ", "JOAO", null, null);

        CardData result = providedCard.merge(defaultCard);

        assertEquals("4111111111111111", result.number());
        assertEquals("11", result.expirationMonth());
        assertEquals("2028", result.expirationYear());
        assertEquals("123", result.securityCode());
        assertEquals("JOAO", result.holderName());
        assertEquals("CPF", result.identificationType());
        assertEquals("19119119100", result.identificationNumber());
    }

    @Test
    @DisplayName("Deve indicar quando o cartão está completo")
    void shouldIndicateWhenCardIsComplete() {
        assertTrue(defaultCard.isComplete());
        assertFalse(new CardData(null, null, null, null, null, null, null).isComplete());
    }
}
