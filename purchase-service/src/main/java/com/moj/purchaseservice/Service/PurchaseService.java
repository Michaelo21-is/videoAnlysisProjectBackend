package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Configuration.RabbitMqConfig;
import com.moj.purchaseservice.Dto.OrderCreditDto;
import com.moj.purchaseservice.Dto.OrderStatusDto;
import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Entity.OrderCredit;
import com.moj.purchaseservice.Repository.OrderAnalyzeContentsRepository;
import com.moj.purchaseservice.Repository.OrderCreditRepository;
import com.moj.purchaseservice.Response.OrderAnalyzeResponse;
import com.moj.purchaseservice.Response.OrderAnalyzeStatusResponse;
import com.moj.purchaseservice.Utils.CalculateCreditToUsd;
import com.moj.purchaseservice.Utils.ContentPricingCalculator;
import com.moj.purchaseservice.enums.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PurchaseService {
    private final RabbitTemplate rabbitTemplate;
    private final OrderAnalyzeContentsRepository orderAnalyzeContentRepository;
    private final SseService sseService;
    private final OrderCreditRepository orderCreditRepository;
    public PurchaseService(OrderAnalyzeContentsRepository orderAnalyzeContentRepository
            , RabbitTemplate rabbitTemplate, SseService sseService, OrderCreditRepository orderCreditRepository) {
        this.orderAnalyzeContentRepository = orderAnalyzeContentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.sseService = sseService;
        this.orderCreditRepository = orderCreditRepository;
    }
    ///
    /// order analyze content request
    ///
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
    ///
    /// order analyze content request
    ///
    public Long orderCredit(UUID userId, OrderCreditDto orderCredit){
        if (orderCredit.getCredit() < 10 || orderCredit.getEmail() == null || orderCredit.getFullName() == null) {
            throw new RuntimeException("Invalid order credit request");
        }
        if (userId == null) {
            throw new RuntimeException("problem with security configurations in api gateway");
        }
        BigDecimal priceInUsd = CalculateCreditToUsd.calculateCreditToUsd(orderCredit.getCredit());
        // api request to stripe with deatils

        OrderCredit order = OrderCredit.builder()
                .credit(orderCredit.getCredit())
                .priceInUsd(priceInUsd)
                .status(OrderStatus.PENDING)
                .userId(userId)
                .build();
        orderCreditRepository.save(order);

        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_CREDIT_EXCHANGE, RabbitMqConfig.ORDER_CREDIT_ROUTING_KEY, order);

        return order.getId();
    }


    ///
    /// order credit
    ///

}
