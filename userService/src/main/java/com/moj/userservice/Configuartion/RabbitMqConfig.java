package com.moj.userservice.Configuartion;

import org.springframework.amqp.core.*;
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
    public static final String ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE = "order-analyze-content-status-exchange";
    public static final String ORDER_ANALYZE_CONTENT_STATUS_ROUTING_KEY = "order-analyze-content-analyzeOrderStatus-routing-key";

    public static final String ORDER_CREDIT_QUEUE = "order-credit-queue";
    public static final String ORDER_CREDIT_STATUS_EXCHANGE = "order-credit-status-exchange";
    public static final String ORDER_CREDIT_STATUS_ROUTING_KEY = "order-credit-analyzeOrderStatus-routing-key";


    public static final String ORDER_ANALYZE_VIDEO_QUEUE = "order-analyze-video-queue";


    public static final String ANALYZE_VIDEO_EXCHANGE = "video-analyze-exchange";

    public static final String ORDER_ANALYZE_VIDEO_STATUS_ROUTING_KEY = "order-analyze-video-analyzeOrderStatus-routing-key";
    public static final String ORDER_ANALYZE_VIDEO_STATUS_QUEUE = "order-analyze-video-status-queue";

    public static final String ANALYZE_VIDEO_ROUTING_KEY = "video-analyze-routing-key";
    public static final String ANALYZE_VIDEO_QUEUE = "video-analyze-queue";

    public static final String FAILED_ANALYZE_VIDEO_QUEUE = "failed-analyze-video-queue";

    public static final String USER_REFUND_QUEUE = "user-refund-queue";
    public static final String USER_REFUND_ROUTING_KEY = "user-refund-routing-key";

    @Bean
    public Queue failedAnalyzeVideoQueue() {
        return QueueBuilder
                .durable(FAILED_ANALYZE_VIDEO_QUEUE)
                .build();
    }

    @Bean
    public Queue orderAnalyzeVideoStatusQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_VIDEO_STATUS_QUEUE)
                .build();
    }
    @Bean
    public DirectExchange analyzeVideoExchange() {
        return new DirectExchange(ANALYZE_VIDEO_EXCHANGE);
    }
    @Bean
    public Binding orderAnalyzeVideoStatusBinding(@Qualifier("orderAnalyzeVideoStatusQueue")
            Queue orderAnalyzeVideoStatusQueue,
            @Qualifier("analyzeVideoExchange")
            DirectExchange orderAnalyzeVideoStatusExchange
    ) {
        return BindingBuilder
                .bind(orderAnalyzeVideoStatusQueue)
                .to(orderAnalyzeVideoStatusExchange)
                .with(ORDER_ANALYZE_VIDEO_STATUS_ROUTING_KEY);
    }

    /*
        order analyze video queue
     */
    @Bean
    public Queue orderAnalyzeVideoQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_VIDEO_QUEUE)
                .build();
    }

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
    public Queue videoAnalyzeQueue() {
        return QueueBuilder
                .durable(ANALYZE_VIDEO_QUEUE)
                .build();
    }
    @Bean
    public Binding videoAnalyzeBinding(@Qualifier("videoAnalyzeQueue") Queue videoAnalyzeQueue
            , @Qualifier("analyzeVideoExchange") DirectExchange analyzeVideoExchange) {
        return BindingBuilder
                .bind(videoAnalyzeQueue)
                .to(analyzeVideoExchange)
                .with(ANALYZE_VIDEO_ROUTING_KEY);
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
    public Queue userRefundQueue() {
        return QueueBuilder
                .durable(USER_REFUND_QUEUE)
                .build();
    }

    @Bean
    public Binding userRefundBinding(@Qualifier("userRefundQueue") Queue userRefundQueue, @Qualifier("analyzeVideoExchange") DirectExchange analyzeVideoExchange) {
        return BindingBuilder
                .bind(userRefundQueue)
                .to(analyzeVideoExchange)
                .with(USER_REFUND_ROUTING_KEY);
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