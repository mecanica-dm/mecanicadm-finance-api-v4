package com.mecanicadm.mecanicadm_api.infra.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    @Bean
    public HeadersExchange financeOutputExchange(
            @Value("${app.rabbitmq.exchange.output:mecanicadm-finance-api-output}") String exchangeName) {
        return new HeadersExchange(exchangeName, true, false);
    }

    @Bean
    public Queue budgetQueue(@Value("${app.rabbitmq.queues.budget:budget}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding generateBudgetBinding(Queue budgetQueue,
                                         HeadersExchange financeOutputExchange,
                                         @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        return BindingBuilder.bind(budgetQueue).to(financeOutputExchange)
                .whereAll(Map.of(eventTypeHeader, GenerateBudgetIntegrationCommand.class.getSimpleName())).match();
    }

    @Bean
    public Binding cancelBudgetBinding(Queue budgetQueue,
                                       HeadersExchange financeOutputExchange,
                                       @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        return BindingBuilder.bind(budgetQueue).to(financeOutputExchange)
                .whereAll(Map.of(eventTypeHeader, CancelBudgetIntegrationCommand.class.getSimpleName())).match();
    }

    @Bean
    public Binding workOrderCreatedBinding(Queue budgetQueue,
                                           HeadersExchange financeOutputExchange,
                                           @Value("${app.rabbitmq.headers.event-type:event-type}") String eventTypeHeader) {
        return BindingBuilder.bind(budgetQueue).to(financeOutputExchange)
                .whereAll(Map.of(eventTypeHeader, WorkOrderCreatedEvent.class.getSimpleName())).match();
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
