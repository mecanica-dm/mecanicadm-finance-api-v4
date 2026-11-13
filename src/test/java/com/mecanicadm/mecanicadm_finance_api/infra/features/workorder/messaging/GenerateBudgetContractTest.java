package com.mecanicadm.mecanicadm_finance_api.infra.features.workorder.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.LaborExecutionStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.enums.WorkOrderBudgetStatus;
import com.mecanicadm.mecanicadm_finance_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateBudgetContractTest {

    private static final String SAGA_PAYLOAD = """
            {"workOrderId":"a06ed083-93b2-4313-b9bc-fc3626b6b6b8","workOrderSnapshot":{
              "totalPrice":720.00,"status":"PENDING","observation":null,"description":"Revisao",
              "client":{"id":"8b3f81e5-e118-4e31-8979-994cba5256e4","name":"Deliveries Express","email":"express@deliver.com","document":"11222333000181","phone":"48999999005"},
              "vehicle":{"licensePlate":"ABC-1234","model":"Volvo FH 460","brand":"Volvo","modelYear":2020},
              "materialItems":[{"id":"11e2c32c-5ae2-42e0-a51a-110183c6eb90","materialId":"5c5450e1-236b-43d9-95a7-96a1a1f73b15","quantity":2,"name":"Filtro de Oleo","brand":"Mann","description":"Filtro","price":45.00,"type":"PART"}],
              "laborItems":[{"id":"267ef2a5-0580-46ec-bae7-0dccc29c8156","workOrderId":"a06ed083-93b2-4313-b9bc-fc3626b6b6b8","laborId":"e8281144-411a-4d37-97d8-1e4eb1e79391","executionStartAt":null,"executionEndAt":null,"status":"AWAITING_EXECUTION","name":"Limpeza de Carbono","price":300.00}]}}
            """;

    @Test
    @DisplayName("Deve ler o GenerateBudgetIntegrationCommand publicado pela saga, com o workOrderSnapshot completo")
    void shouldReadCommandPublishedBySaga() throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

        GenerateBudgetIntegrationCommand command = mapper.readValue(SAGA_PAYLOAD, GenerateBudgetIntegrationCommand.class);

        assertThat(command.workOrderId().toString()).isEqualTo("a06ed083-93b2-4313-b9bc-fc3626b6b6b8");
        assertThat(command.workOrderSnapshot().totalPrice()).isEqualByComparingTo("720.00");
        assertThat(command.workOrderSnapshot().status()).isEqualTo(WorkOrderBudgetStatus.PENDING);
        assertThat(command.workOrderSnapshot().client().name()).isEqualTo("Deliveries Express");
        assertThat(command.workOrderSnapshot().vehicle().modelYear()).isEqualTo((short) 2020);
        assertThat(command.workOrderSnapshot().materialItems().get(0).price()).isEqualByComparingTo("45.00");
        assertThat(command.workOrderSnapshot().materialItems().get(0).quantity()).isEqualTo(2);
        assertThat(command.workOrderSnapshot().laborItems().get(0).status()).isEqualTo(LaborExecutionStatus.AWAITING_EXECUTION);
        assertThat(command.workOrderSnapshot().laborItems().get(0).price()).isEqualByComparingTo("300.00");
    }
}
