package com.moj.notificationservice.Configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String QUEUE_NAME = "two_factor_queue";

    //checking if the queue is exist if not create it
    @Bean
    public Queue twoFactorQueue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .build();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}