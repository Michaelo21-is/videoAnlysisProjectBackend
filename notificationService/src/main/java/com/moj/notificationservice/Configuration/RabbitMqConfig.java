package com.moj.notificationservice.Configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String TWO_FACTOR_QUEUE = "two-factor-queue";
    public static final String PASSWORD_RESET_QUEUE = "password-reset-queue";
    //checking if the queue is exist if not create it
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
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}