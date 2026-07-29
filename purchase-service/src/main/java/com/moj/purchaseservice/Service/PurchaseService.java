package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Configuration.RabbitMqConfig;
import com.moj.purchaseservice.Dto.OrderCreditDto;
import com.moj.purchaseservice.Dto.OrderCreditStatusDto;
import com.moj.purchaseservice.Dto.OrderStatusDto;
import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Entity.OrderCredit;
import com.moj.purchaseservice.Repository.OrderAnalyzeContentsRepository;
import com.moj.purchaseservice.Repository.OrderCreditRepository;
import com.moj.purchaseservice.Response.OrderAnalyzeResponse;
import com.moj.purchaseservice.Response.OrderResponse;
import com.moj.purchaseservice.Response.OrderCreditResponse;
import com.moj.purchaseservice.Response.ResolvePackageResponse;
import com.moj.purchaseservice.Utils.ContentPricingCalculator;
import com.moj.purchaseservice.Utils.PaddleResolvePackage;
import com.moj.purchaseservice.enums.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PurchaseService {
    private final RabbitTemplate rabbitTemplate;
    private final OrderAnalyzeContentsRepository orderAnalyzeContentRepository;
    private final SseOrderAnalyzeContentService sseOrderAnalyzeContentService;
    private final OrderCreditRepository orderCreditRepository;
    private final PaddleResolvePackage paddleResolvePackage;
    private final SseOrderCreditService sseOrderCreditService;
    public PurchaseService(OrderAnalyzeContentsRepository orderAnalyzeContentRepository
            , RabbitTemplate rabbitTemplate, SseOrderAnalyzeContentService sseOrderAnalyzeContentService,
               OrderCreditRepository orderCreditRepository, PaddleResolvePackage paddleResolvePackage,
           SseOrderCreditService sseOrderCreditService) {
        this.orderAnalyzeContentRepository = orderAnalyzeContentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.sseOrderAnalyzeContentService = sseOrderAnalyzeContentService;
        this.orderCreditRepository = orderCreditRepository;
        this.paddleResolvePackage = paddleResolvePackage;
        this.sseOrderCreditService = sseOrderCreditService;
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
        OrderResponse response;

        switch (orderStatusDto.getStatus()) {
            case SUCCEED -> {
                order.setStatus(OrderAnalyzeVideoStatus.PURCHASED);
                order.setPurchasedAt(Instant.now());
                orderAnalyzeContentRepository.save(order);
                response = OrderResponse.builder()
                        .status(Status.SUCCEED)
                        .orderId(orderStatusDto.getOrderId())
                        .message("Order analyze content succeeded")
                        .build();
            }

            case PAYMENT_FAILED -> {
                order.setStatus(OrderAnalyzeVideoStatus.PAYMENT_FAILED);
                orderAnalyzeContentRepository.save(order);
                response = OrderResponse.builder()
                        .orderId(orderStatusDto.getOrderId())
                        .status(Status.PAYMENT_FAILED)
                        .message("Not enough credits in your account")
                        .build();
            }
            case SERVER_FAILED ->{
                order.setStatus(OrderAnalyzeVideoStatus.SERVER_FAILED);
                orderAnalyzeContentRepository.save(order);
                response = OrderResponse.builder()
                        .orderId(orderStatusDto.getOrderId())
                        .status(Status.PAYMENT_FAILED)
                        .message("Something went wrong with our server, please try again later")
                        .build();
            }

            default -> {
                return;
            }
        }
        sseOrderAnalyzeContentService.sendFinalStatus(orderStatusDto.getOrderId(), response);
    }
    ///
    /// order analyze content request
    ///
    public Long orderCredit(UUID userId, OrderCreditDto orderCredit){
        if (userId == null) {
            throw new RuntimeException("problem with security configurations in api gateway");
        }
        ResolvePackageResponse response = paddleResolvePackage.resolvePriceId(orderCredit.getProductId()) ;
        OrderCredit order = OrderCredit.builder()
                .credit(response.getCredit())
                .priceInUsd(response.getPriceInUsd())
                .status(OrderStatus.PENDING)
                .userId(userId)
                .build();
        orderCreditRepository.save(order);


        return order.getId();
    }
    public void orderCreditPurchasedSuccessfully(Long orderId) {
        OrderCredit order = orderCreditRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.PURCHASED);
        orderCreditRepository.save(order);
        OrderCreditResponse response = OrderCreditResponse.builder()
                .orderId(orderId)
                .email(null)
                .FullName(null)
                .credit(order.getCredit())
                .priceInUsd(order.getPriceInUsd())
                .userId(order.getUserId())
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_CREDIT_EXCHANGE, RabbitMqConfig.ORDER_CREDIT_ROUTING_KEY, response);
    }
    public void orderCreditPurchasedFailed(Long orderId) {
        OrderCredit order = orderCreditRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderCreditRepository.save(order);
        // response to the front for user message

    }
    @RabbitListener(queues = RabbitMqConfig.ORDER_CREDIT_STATUS_QUEUE)
    public void orderCreditStatus(OrderCreditStatusDto orderCreditStatusDto) {
        if (orderCreditStatusDto.getOrderCreditStatus() == null) {
            return;
        }
        OrderCredit orderCredit = orderCreditRepository.findById(orderCreditStatusDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        switch (orderCreditStatusDto.getOrderCreditStatus()) {
            case SUCCEED -> {
                orderCredit.setStatus(OrderStatus.PURCHASED);
                orderCreditRepository.save(orderCredit);
                OrderResponse response = OrderResponse.builder()
                        .orderId(orderCreditStatusDto.getOrderId())
                        .status(Status.SUCCEED)
                        .message("Order credit succeeded")
                        .build();
                sseOrderCreditService.sendFinalStatus(orderCreditStatusDto.getOrderId(), response);
            }
            case PAYMENT_FAILED -> {
                orderCredit.setStatus(OrderStatus.PAYMENT_FAILED);
                orderCreditRepository.save(orderCredit);
                OrderResponse response = OrderResponse.builder()
                        .orderId(orderCreditStatusDto.getOrderId())
                        .status(Status.PAYMENT_FAILED)
                        .message("Not enough credits in your account")
                        .build();
                sseOrderCreditService.sendFinalStatus(orderCreditStatusDto.getOrderId(), response);
            }
            default -> {
                orderCredit.setStatus(OrderStatus.SERVER_FAILED);
                orderCreditRepository.save(orderCredit);
                OrderResponse response = OrderResponse.builder()
                        .orderId(orderCreditStatusDto.getOrderId())
                        .status(Status.SERVER_FAILED)
                        .message("Something went wrong with our server, please try again later")
                        .build();
                sseOrderCreditService.sendFinalStatus(orderCreditStatusDto.getOrderId(), response);
            }
        }
    }
    ///
    /// order credit
    ///

}
