package com.moj.notificationservice.Configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String TWO_FACTOR_QUEUE =
            "two-factor-queue";

    public static final String PASSWORD_RESET_QUEUE =
            "password-reset-queue";

    /*
     * This queue belongs only to notification-service.
     */
    public static final String ORDER_CREDIT_STATUS_QUEUE =
            "order-credit-notification-status-queue";

    /*
     * Must be identical to the exchange and routing key
     * used by user-service and purchase-service.
     */
    public static final String ORDER_CREDIT_STATUS_EXCHANGE =
            "order-credit-status-exchange";

    public static final String ORDER_CREDIT_STATUS_ROUTING_KEY =
            "order-credit-analyzeOrderStatus-routing-key";

    @Bean
    public Queue twoFactorQueue() {
        return QueueBuilder
                .durable(TWO_FACTOR_QUEUE)
                .build();
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder
                .durable(PASSWORD_RESET_QUEUE)
                .build();
    }

    @Bean
    public DirectExchange orderCreditStatusExchange() {
        return new DirectExchange(
                ORDER_CREDIT_STATUS_EXCHANGE
        );
    }

    @Bean
    public Queue orderCreditStatusQueue() {
        return QueueBuilder
                .durable(ORDER_CREDIT_STATUS_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreditStatusBinding(
            @Qualifier("orderCreditStatusQueue") Queue queue,
            @Qualifier("orderCreditStatusExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_CREDIT_STATUS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}