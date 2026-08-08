package com.moj.userservice.Utils;

import com.moj.userservice.Configuartion.RabbitMqConfig;
import com.moj.userservice.Dto.OrderAnalyzeVideoDto;
import com.moj.userservice.Dto.OrderCreditDto;
import com.moj.userservice.Enums.AnalyzeOrderStatus;
import com.moj.userservice.Enums.OrderCreditStatus;
import com.moj.userservice.Response.OrderAnalyzeVideoResponse;
import com.moj.userservice.Response.OrderCreditResponse;
import com.moj.userservice.Response.OrderVideoAnalysisStatusResponse;
import com.moj.userservice.Response.PurchaseWithCreditResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SendToQueue {
    private RabbitTemplate rabbitTemplate;
    public SendToQueue(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    public void sendOrderStatus(Long orderId, AnalyzeOrderStatus analyzeOrderStatus) {
        PurchaseWithCreditResponse response =
                PurchaseWithCreditResponse.builder()
                        .orderId(orderId)
                        .analyzeOrderStatus(analyzeOrderStatus)
                        .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE,
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_ROUTING_KEY,
                response
        );
    }
    public void sendOrderCreditStatus(OrderCreditDto orderCreditDto, OrderCreditStatus  orderCreditStatus) {
        OrderCreditResponse response = OrderCreditResponse.builder()
                .orderId(orderCreditDto.getOrderId())
                .creditAdded(orderCreditDto.getCredit())
                .priceInUsd(orderCreditDto.getPriceInUsd())
                .email(orderCreditDto.getEmail())
                .fullName(orderCreditDto.getFullName())
                .orderCreditStatus(orderCreditStatus)
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_CREDIT_STATUS_EXCHANGE,
                RabbitMqConfig.ORDER_CREDIT_STATUS_ROUTING_KEY,
                response);
    }
    public void sendOrderAnalyzeStatus(OrderVideoAnalysisStatusResponse response){
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ANALYZE_VIDEO_EXCHANGE,
                RabbitMqConfig.ORDER_ANALYZE_VIDEO_STATUS_ROUTING_KEY,
                response
        );
    }
    public void sendOrderToAnalyzeVideo(OrderAnalyzeVideoResponse response){
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ANALYZE_VIDEO_EXCHANGE,
                RabbitMqConfig.ANALYZE_VIDEO_ROUTING_KEY,
                response
        );
    }
}
