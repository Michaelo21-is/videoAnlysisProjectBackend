package com.moj.userservice.Utils;

import com.moj.userservice.Configuartion.RabbitMqConfig;
import com.moj.userservice.Enums.Status;
import com.moj.userservice.Response.PurchaseResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendToQueue {
    private RabbitTemplate rabbitTemplate;
    public SendToQueue(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    public void sendOrderStatus(Long orderId, UUID userId, Status status) {
        PurchaseResponse response =
                PurchaseResponse.builder()
                        .orderId(orderId)
                        .status(status)
                        .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_EXCHANGE,
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_ROUTING_KEY,
                response
        );
    }
}
