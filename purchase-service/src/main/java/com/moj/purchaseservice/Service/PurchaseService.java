package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Configuration.RabbitMqConfig;
import com.moj.purchaseservice.Entity.OrderAnalyzeVideos;
import com.moj.purchaseservice.Repository.OrderAnalyzeVideosRepository;
import com.moj.purchaseservice.Response.OrderAnalyzeResponse;
import com.moj.purchaseservice.Utils.ContentPricingCalculator;
import com.moj.purchaseservice.enums.ContentType;
import com.moj.purchaseservice.enums.SumOfContent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PurchaseService {
    private final RabbitTemplate rabbitTemplate;
    private final OrderAnalyzeVideosRepository orderAnalyzeVideosRepository;
    public PurchaseService(OrderAnalyzeVideosRepository orderAnalyzeVideosRepository, RabbitTemplate rabbitTemplate) {
        this.orderAnalyzeVideosRepository = orderAnalyzeVideosRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void orderAnalyzeContent(UUID userId, SumOfContent sumOfContent, ContentType contentType) {
        Long credit = ContentPricingCalculator.calculateCreditCost(sumOfContent, contentType);
        OrderAnalyzeVideos orderAnalyzeVideos = OrderAnalyzeVideos.builder()
                .userId(userId)
                .sumOfContent(sumOfContent)
                .creditCost(credit)
                .build();
        orderAnalyzeVideosRepository.save(orderAnalyzeVideos);
        OrderAnalyzeResponse response = OrderAnalyzeResponse.builder()
                .orderId(orderAnalyzeVideos.getId())
                .creditCost(credit)
                .sumOfContent(sumOfContent)
                .contentType(contentType)
                .userId(userId)
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_ANALYZE_CONTENT_EXCHANGE, RabbitMqConfig.ORDER_ANALYZE_CONTENT_ROUTING_KEY, response);
    }
}
