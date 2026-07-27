package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Configuration.RabbitMqConfig;
import com.moj.purchaseservice.Dto.OrderStatusDto;
import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Repository.OrderAnalyzeContentsRepository;
import com.moj.purchaseservice.Response.OrderAnalyzeResponse;
import com.moj.purchaseservice.Response.OrderAnalyzeStatusResponse;
import com.moj.purchaseservice.Utils.ContentPricingCalculator;
import com.moj.purchaseservice.enums.ContentType;
import com.moj.purchaseservice.enums.OrderAnalyzeVideoStatus;
import com.moj.purchaseservice.enums.Status;
import com.moj.purchaseservice.enums.SumOfContent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PurchaseService {
    private final RabbitTemplate rabbitTemplate;
    private final OrderAnalyzeContentsRepository orderAnalyzeContentRepository;
    private final SseService sseService;
    public PurchaseService(OrderAnalyzeContentsRepository orderAnalyzeContentRepository
            , RabbitTemplate rabbitTemplate, SseService sseService) {
        this.orderAnalyzeContentRepository = orderAnalyzeContentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.sseService = sseService;
    }

    public Long orderAnalyzeContent(UUID userId, SumOfContent sumOfContent, ContentType contentType) {
        Long credit = ContentPricingCalculator.calculateCreditCost(sumOfContent, contentType);
        OrderAnalyzeContents orderAnalyzeContents = OrderAnalyzeContents.builder()
                .userId(userId)
                .status(OrderAnalyzeVideoStatus.PENDING)
                .sumOfContent(sumOfContent)
                .contentType(contentType)
                .creditCost(credit)
                .build();
        orderAnalyzeContentRepository.save(orderAnalyzeContents);
        OrderAnalyzeResponse response = OrderAnalyzeResponse.builder()
                .orderId(orderAnalyzeContents.getId())
                .creditCost(credit)
                .sumOfContent(sumOfContent)
                .contentType(contentType)
                .userId(userId)
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_ANALYZE_CONTENT_EXCHANGE, RabbitMqConfig.ORDER_ANALYZE_CONTENT_ROUTING_KEY, response);
        return orderAnalyzeContents.getId();
    }
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_QUEUE)
    public void orderAnalyzeContentStatus(OrderStatusDto orderStatusDto) {
        if (orderStatusDto.getStatus() == null) {
            return;
        }

        OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(orderStatusDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        OrderAnalyzeStatusResponse response;

        switch (orderStatusDto.getStatus()) {
            case SUCCEED -> {
                order.setStatus(OrderAnalyzeVideoStatus.PURCHASED);
                order.setPurchasedAt(Instant.now());
                orderAnalyzeContentRepository.save(order);
                response = OrderAnalyzeStatusResponse.builder()
                        .status(Status.SUCCEED)
                        .orderId(orderStatusDto.getOrderId())
                        .message("Order analyze content succeeded")
                        .build();
            }

            case PAYMENT_FAILED -> {
                order.setStatus(OrderAnalyzeVideoStatus.PAYMENT_FAILED);
                orderAnalyzeContentRepository.save(order);
                response = OrderAnalyzeStatusResponse.builder()
                        .orderId(orderStatusDto.getOrderId())
                        .status(Status.PAYMENT_FAILED)
                        .message("Not enough credits in your account")
                        .build();
            }
            case SERVER_FAILED ->{
                order.setStatus(OrderAnalyzeVideoStatus.SERVER_FAILED);
                orderAnalyzeContentRepository.save(order);
                response = OrderAnalyzeStatusResponse.builder()
                        .orderId(orderStatusDto.getOrderId())
                        .status(Status.PAYMENT_FAILED)
                        .message("Something went wrong with our server, please try again later")
                        .build();
            }

            default -> {
                return;
            }
        }
        sseService.sendFinalStatus(orderStatusDto.getOrderId(), response);
    }
}
