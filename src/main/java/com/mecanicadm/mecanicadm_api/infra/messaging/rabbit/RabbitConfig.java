package com.mecanicadm.mecanicadm_api.infra.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.CancelBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.GenerateBudgetIntegrationCommand;
import com.mecanicadm.mecanicadm_api.core.workorder.domain.event.WorkOrderCreatedEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    @Bean
    public HeadersExchange financeOutputExchange(
            @Value("${app.rabbitmq.exchange.output:finance-output}") String exchangeName) {
        return new HeadersExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange financeDeadLetterExchange(
            @Value("${app.rabbitmq.exchange.dead-letter:finance-dlx}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue budgetQueue(
            @Value("${app.rabbitmq.queues.budget:budget}") String queueName,
            @Value("${app.rabbitmq.exchange.dead-letter:finance-dlx}") String deadLetterExchange) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(queueName)
                .build();
    }

    @Bean
    public Queue budgetDeadLetterQueue(
            @Value("${app.rabbitmq.queues.budget-dlq:budget.dlq}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding budgetDeadLetterBinding(Queue budgetDeadLetterQueue,
                                           DirectExchange financeDeadLetterExchange,
                                           @Value("${app.rabbitmq.queues.budget:budget}") String routingKey) {
        return BindingBuilder.bind(budgetDeadLetterQueue).to(financeDeadLetterExchange).with(routingKey);
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
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
