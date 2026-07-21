package com.moj.authservice.Configuration;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String AUTH_NOTIFICATION_EXCHANGE = "auth-notification-exchange";

    public static final String TWO_FACTOR_QUEUE = "two-factor-queue";

    public static final String PASSWORD_RESET_QUEUE = "password-reset-queue";

    public static final String TWO_FACTOR_ROUTING_KEY = "auth.two-factor";

    public static final String PASSWORD_RESET_ROUTING_KEY = "auth.password-reset";

    @Bean
    public DirectExchange authNotificationExchange() {
        return new DirectExchange(AUTH_NOTIFICATION_EXCHANGE);
    }

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
    public Binding twoFactorBinding(
            @Qualifier("twoFactorQueue") Queue queue,
            @Qualifier("authNotificationExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(TWO_FACTOR_ROUTING_KEY);
    }

    @Bean
    public Binding passwordResetBinding(
            @Qualifier("passwordResetQueue") Queue queue,
            @Qualifier("authNotificationExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(PASSWORD_RESET_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}