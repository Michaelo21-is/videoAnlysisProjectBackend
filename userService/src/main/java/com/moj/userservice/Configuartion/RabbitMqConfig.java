package com.moj.userservice.Configuartion;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_ANALYZE_CONTENT_QUEUE = "order-analyze-content-queue";

    public static final String ORDER_ANALYZE_CONTENT_STATUS_QUEUE = "order-analyze-content-status-queue";

    public static final String ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE = "order-analyze-content-status-exchange";

    public static final String ORDER_ANALYZE_CONTENT_STATUS_ROUTING_KEY = "order-analyze-content-status-routing-key";



    @Bean
    public Queue orderAnalyzeContentQueue() {
        return new Queue(ORDER_ANALYZE_CONTENT_QUEUE);
    }

    @Bean
    public DirectExchange orderAnalyzeContentStatusExchange() {
        return new DirectExchange(
                ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE
        );
    }

    @Bean
    public Queue orderAnalyzeContentStatusQueue() {
        return new Queue(ORDER_ANALYZE_CONTENT_STATUS_QUEUE);
    }

    @Bean
    public Binding orderAnalyzeContentStatusBinding(
            @Qualifier("orderAnalyzeContentStatusQueue") Queue queue,
            @Qualifier("orderAnalyzeContentStatusExchange")
            DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_ANALYZE_CONTENT_STATUS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}