package com.moj.purchaseservice.Configuration;

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
    public static final String ORDER_ANALYZE_CONTENT_STATUS_QUEUE = "order-analyze-content-status-queue";

    public static final String ORDER_ANALYZE_CONTENT_EXCHANGE = "order-analyze-content-exchange";
    public static final String ORDER_ANALYZE_CONTENT_QUEUE = "order-analyze-content-queue";
    public static final String ORDER_ANALYZE_CONTENT_ROUTING_KEY = "order.analyze.content";

    public static final String ORDER_CREDIT_QUEUE = "order-credit-queue";
    public static final String ORDER_CREDIT_EXCHANGE = "order-credit-exchange";
    public static final String ORDER_CREDIT_ROUTING_KEY = "order.credit";

    @Bean
    public Queue orderAnalyzeContentStatusQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_CONTENT_STATUS_QUEUE)
                .build();
    }



    @Bean
    public DirectExchange orderAnalyzeContentExchange() {return new DirectExchange(ORDER_ANALYZE_CONTENT_EXCHANGE);
    }

    @Bean
    public Queue orderAnalyzeContentQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_CONTENT_QUEUE)
                .build();
    }

    @Bean
    public Binding orderAnalyzeContentBinding(@Qualifier("orderAnalyzeContentQueue") Queue queue, @Qualifier("orderAnalyzeContentExchange") DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_ANALYZE_CONTENT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderCreditExchange() {return new DirectExchange(ORDER_CREDIT_EXCHANGE);}

    @Bean
    public Queue orderCreditQueue() {
        return QueueBuilder
                .durable(ORDER_CREDIT_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreditBinding(@Qualifier("orderCreditQueue") Queue queue, @Qualifier("orderCreditExchange") DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_CREDIT_ROUTING_KEY);
    }


    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}