package com.moj.userservice.Configuartion;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    /*
     * Analyze-content request:
     * purchase-service publishes,
     * user-service consumes.
     */
    public static final String ORDER_ANALYZE_CONTENT_QUEUE =
            "order-analyze-content-queue";

    /*
     * Analyze-content result:
     * user-service publishes,
     * purchase-service consumes.
     */
    public static final String ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE =
            "order-analyze-content-status-exchange";

    public static final String ORDER_ANALYZE_CONTENT_STATUS_ROUTING_KEY =
            "order-analyze-content-analyzeOrderStatus-routing-key";

    /*
     * Credit purchase request:
     * purchase-service publishes,
     * user-service consumes.
     */
    public static final String ORDER_CREDIT_QUEUE =
            "order-credit-queue";

    /*
     * Credit update result:
     * user-service publishes.
     *
     * purchase-service and notification-service each have
     * their own queue connected to this exchange.
     */
    public static final String ORDER_CREDIT_STATUS_EXCHANGE =
            "order-credit-status-exchange";

    public static final String ORDER_CREDIT_STATUS_ROUTING_KEY =
            "order-credit-analyzeOrderStatus-routing-key";

    @Bean
    public Queue orderAnalyzeContentQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_CONTENT_QUEUE)
                .build();
    }

    @Bean
    public Queue orderCreditQueue() {
        return QueueBuilder
                .durable(ORDER_CREDIT_QUEUE)
                .build();
    }

    @Bean
    public DirectExchange orderAnalyzeContentStatusExchange() {
        return new DirectExchange(
                ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE
        );
    }

    @Bean
    public DirectExchange orderCreditStatusExchange() {
        return new DirectExchange(
                ORDER_CREDIT_STATUS_EXCHANGE
        );
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}