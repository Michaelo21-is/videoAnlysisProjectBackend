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


    public static final String ORDER_ANALYZE_CONTENT_EXCHANGE = "order-analyze-content-exchange";
    public static final String ORDER_ANALYZE_CONTENT_QUEUE = "order-analyze-content-queue";
    public static final String ORDER_ANALYZE_CONTENT_ROUTING_KEY = "order.analyze-content.routing-key";


    public static final String ORDER_CREDIT_QUEUE = "order-credit-queue";
    public static final String ORDER_CREDIT_EXCHANGE = "order-credit-exchange";
    public static final String ORDER_CREDIT_ROUTING_KEY = "order.credit";


    public static final String ORDER_CREDIT_STATUS_QUEUE = "order-credit-purchase-status-queue";
    public static final String ORDER_CREDIT_STATUS_EXCHANGE = "order-credit-status-exchange";
    public static final String ORDER_CREDIT_STATUS_ROUTING_KEY = "order-credit-analyzeOrderStatus-routing-key";

    public static final String ANALYZE_VIDEO_EXCHANGE = "video-analyze-exchange";
    public static final String ORDER_ANALYZE_VIDEO_QUEUE = "order-analyze-video-queue";
    public static final String ORDER_ANALYZE_VIDEO_ROUTING_KEY = "order.analyze.video";

    public static final String ORDER_ANALYZE_VIDEO_STATUS_QUEUE = "order-analyze-video-status-queue";

    public static final String ANALYZE_VIDEO_QUEUE = "analyze-video-response-queue";

    public static final String SCRAPING_FINISHED_QUEUE = "scraping-finished-queue";

    public static final String FAILED_ANALYZE_VIDEO_QUEUE = "failed-analyze-video-queue";
    public static final String FAILED_ANALYZE_VIDEO_ROUTING_KEY = "failed.analyze.video";

    public static final String USER_REFUND_QUEUE = "user-refund-queue";



    @Bean
    public Queue userRefundQueue() {
        return QueueBuilder
                .durable(USER_REFUND_QUEUE)
                .build();
    }

    @Bean
    public Queue analyzeVideoQueue() {
        return QueueBuilder
                .durable(ANALYZE_VIDEO_QUEUE)
                .build();
    }

    @Bean
    public Queue scrapingFinishedQueue() {
        return QueueBuilder
                .durable(SCRAPING_FINISHED_QUEUE)
                .build();
    }

    @Bean
    public Queue orderAnalyzeVideoStatusQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_VIDEO_STATUS_QUEUE)
                .build();
    }

    @Bean
    public DirectExchange orderAnalyzeContentExchange() {
        return new DirectExchange(
                ORDER_ANALYZE_CONTENT_EXCHANGE
        );
    }

    @Bean
    public Queue orderAnalyzeContentQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_CONTENT_QUEUE)
                .build();
    }

    @Bean
    public Binding orderAnalyzeContentBinding(
            @Qualifier("orderAnalyzeContentQueue") Queue queue,
            @Qualifier("orderAnalyzeContentExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_ANALYZE_CONTENT_ROUTING_KEY);
    }






    @Bean
    public DirectExchange orderCreditExchange() {
        return new DirectExchange(
                ORDER_CREDIT_EXCHANGE
        );
    }

    @Bean
    public Queue orderCreditQueue() {
        return QueueBuilder
                .durable(ORDER_CREDIT_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreditBinding(
            @Qualifier("orderCreditQueue") Queue queue,
            @Qualifier("orderCreditExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_CREDIT_ROUTING_KEY);
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
    public DirectExchange orderAnalyzeVideoExchange() {
        return new DirectExchange(
                ANALYZE_VIDEO_EXCHANGE
        );
    }
    @Bean
    public Queue orderAnalyzeVideoQueue() {
        return QueueBuilder
                .durable(ORDER_ANALYZE_VIDEO_QUEUE)
                .build();
    }
    @Bean
    public Binding orderAnalyzeVideoBinding(@Qualifier("orderAnalyzeVideoQueue") Queue queue, @Qualifier("orderAnalyzeVideoExchange") DirectExchange exchange){
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ORDER_ANALYZE_VIDEO_ROUTING_KEY);
    }
    @Bean
    public Queue failedAnalyzeVideoQueue() {
        return QueueBuilder
                .durable(FAILED_ANALYZE_VIDEO_QUEUE)
                .build();
    }
    @Bean
    public Binding failedAnalyzeVideoBinding(@Qualifier("failedAnalyzeVideoQueue") Queue queue, @Qualifier("orderAnalyzeVideoExchange") DirectExchange exchange){
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(FAILED_ANALYZE_VIDEO_ROUTING_KEY);
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