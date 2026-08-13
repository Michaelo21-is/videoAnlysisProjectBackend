package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Configuration.RabbitMqConfig;
import com.moj.purchaseservice.Dto.*;
import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Entity.OrderAnalyzeVideo;
import com.moj.purchaseservice.Entity.OrderCredit;
import com.moj.purchaseservice.Repository.OrderAnalyzeContentsRepository;
import com.moj.purchaseservice.Repository.OrderAnalyzeVideoRepository;
import com.moj.purchaseservice.Repository.OrderCreditRepository;
import com.moj.purchaseservice.Response.*;
import com.moj.purchaseservice.Utils.ContentPricingCalculator;
import com.moj.purchaseservice.Utils.PaddleResolvePackage;
import com.moj.purchaseservice.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class PurchaseService {

    private final RabbitTemplate rabbitTemplate;
    private final OrderAnalyzeContentsRepository orderAnalyzeContentRepository;
    private final SseOrderAnalyzeContentService sseOrderAnalyzeContentService;
    private final OrderCreditRepository orderCreditRepository;
    private final PaddleResolvePackage paddleResolvePackage;
    private final SseOrderCreditService sseOrderCreditService;
    private final OrderAnalyzeVideoRepository orderAnalyzeVideoRepository;
    private final Long costOfOrderVideoAnalyze;
    private final SseOrderAnalyzeVideoService sseOrderAnalyzeVideoService;
    private final FileService fileService;

    public PurchaseService(
            OrderAnalyzeContentsRepository orderAnalyzeContentRepository,
            RabbitTemplate rabbitTemplate,
            SseOrderAnalyzeContentService sseOrderAnalyzeContentService,
            OrderCreditRepository orderCreditRepository,
            PaddleResolvePackage paddleResolvePackage,
            SseOrderCreditService sseOrderCreditService,
            @Value("${video-analysis.credit-cost}") Long costOfOrderVideoAnalyze,
            OrderAnalyzeVideoRepository orderAnalyzeVideoRepository,
            SseOrderAnalyzeVideoService sseOrderAnalyzeVideoService,
            FileService fileService
    ) {
        this.orderAnalyzeContentRepository = orderAnalyzeContentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.sseOrderAnalyzeContentService = sseOrderAnalyzeContentService;
        this.orderCreditRepository = orderCreditRepository;
        this.paddleResolvePackage = paddleResolvePackage;
        this.sseOrderCreditService = sseOrderCreditService;
        this.costOfOrderVideoAnalyze = costOfOrderVideoAnalyze;
        this.orderAnalyzeVideoRepository = orderAnalyzeVideoRepository;
        this.sseOrderAnalyzeVideoService = sseOrderAnalyzeVideoService;
        this.fileService = fileService;
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

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_EXCHANGE,
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_ROUTING_KEY,
                response
        );

        return orderAnalyzeContents.getId();
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_QUEUE)
    public void orderAnalyzeContentStatus(OrderAnalyzeVideoStatusDto orderStatusDto) {
        if (orderStatusDto.getStatus() == null) {
            return;
        }

        OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(orderStatusDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderResponse response;

        switch (orderStatusDto.getStatus()) {
            case PURCHASED -> {
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

            case SERVER_FAILED -> {
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

        sseOrderAnalyzeContentService.sendFinalStatus(
                orderStatusDto.getOrderId(),
                response
        );
    }

    ///
    /// order credit
    ///
    public Long orderCredit(UUID userId, OrderCreditDto orderCredit) {
        if (userId == null) {
            throw new RuntimeException("problem with security configurations in api gateway");
        }

        ResolvePackageResponse response =
                paddleResolvePackage.resolvePriceId(orderCredit.getProductId());

        log.info(
                "Resolved package: credit={}, priceInUsd={}",
                response.getCredit(),
                response.getPriceInUsd()
        );

        OrderCredit order = OrderCredit.builder()
                .credit(response.getCredit())
                .priceInUsd(response.getPriceInUsd())
                .status(OrderStatus.PENDING)
                .userId(userId)
                .build();

        orderCreditRepository.save(order);

        return order.getId();
    }

    @Transactional
    public void deleteOrderCredit(Long orderId, UUID userId) {
        int result = orderCreditRepository.deleteByIdAndUserIdAndStatus(
                orderId,
                userId,
                OrderStatus.PENDING
        );

        if (result == 0) {
            log.error(
                    "cannot delete the order credit with order id: {} and user id: {}",
                    orderId,
                    userId
            );
            return;
        }

        sseOrderCreditService.closeConnection(orderId);
    }

    public void orderCreditPurchasedSuccessfully(Long orderId) {
        log.info("order credit purchased successfully");

        OrderCredit order = orderCreditRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PURCHASED);
        orderCreditRepository.save(order);

        log.info("order credit status updated to purchased");

        OrderCreditResponse response = OrderCreditResponse.builder()
                .orderId(orderId)
                .email(null)
                .FullName(null)
                .credit(order.getCredit())
                .priceInUsd(order.getPriceInUsd())
                .userId(order.getUserId())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_CREDIT_EXCHANGE,
                RabbitMqConfig.ORDER_CREDIT_ROUTING_KEY,
                response
        );
    }

    public void orderCreditPurchasedFailed(Long orderId) {
        OrderCredit order = orderCreditRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderCreditRepository.save(order);
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREDIT_STATUS_QUEUE)
    public void orderCreditStatus(OrderCreditStatusDto orderCreditStatusDto) {
        if (orderCreditStatusDto.getOrderCreditStatus() == null) {
            return;
        }

        OrderCredit orderCredit = orderCreditRepository
                .findById(orderCreditStatusDto.getOrderId())
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

                sseOrderCreditService.sendFinalStatus(
                        orderCreditStatusDto.getOrderId(),
                        response
                );
            }

            case PAYMENT_FAILED -> {
                orderCredit.setStatus(OrderStatus.PAYMENT_FAILED);
                orderCreditRepository.save(orderCredit);

                OrderResponse response = OrderResponse.builder()
                        .orderId(orderCreditStatusDto.getOrderId())
                        .status(Status.PAYMENT_FAILED)
                        .message("Not enough credits in your account")
                        .build();

                sseOrderCreditService.sendFinalStatus(
                        orderCreditStatusDto.getOrderId(),
                        response
                );
            }

            default -> {
                orderCredit.setStatus(OrderStatus.SERVER_FAILED);
                orderCreditRepository.save(orderCredit);

                OrderResponse response = OrderResponse.builder()
                        .orderId(orderCreditStatusDto.getOrderId())
                        .status(Status.SERVER_FAILED)
                        .message("Something went wrong with our server, please try again later")
                        .build();

                sseOrderCreditService.sendFinalStatus(
                        orderCreditStatusDto.getOrderId(),
                        response
                );
            }
        }
    }

    /*
        order analyze video
     */

    public Long orderAnalyzeVideo(
            UUID userId,
            OrderAnalyzeVideoDto orderAnalyzeVideoDto
    ) {

        log.info("order analyze video request received");

        if (userId == null) {
            log.error(
                    "userId is null in order analyze video check security configurations in api gateway"
            );

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "problem with server try again later"
            );
        }

        boolean hasVideoFile =
                orderAnalyzeVideoDto.getVideoFile() != null
                        && !orderAnalyzeVideoDto.getVideoFile().isEmpty();

        boolean hasVideoLink =
                orderAnalyzeVideoDto.getVideoLink() != null
                        && !orderAnalyzeVideoDto.getVideoLink().isEmpty();

        if (!hasVideoFile && !hasVideoLink) {
            log.error("orderAnalyzeVideoDto is null in order analyze video");

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "you should provide video file or video url to analyze"
            );
        }

        if (hasVideoFile && hasVideoLink) {
            log.error(
                    "orderAnalyzeVideoDto has both video file and video link"
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "you should provide video file or video url to analyze"
            );
        }

        String video_s3_url = null;

        if (hasVideoFile) {
            try {
                video_s3_url =
                        fileService.handleVideoFile(orderAnalyzeVideoDto.getVideoFile());
            } catch (Exception e) {
                log.error("error in uploading video file to s3", e);

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e.getMessage()
                );
            }
        }

        OrderAnalyzeVideo orderAnalyzeVideo = OrderAnalyzeVideo.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .build();

        orderAnalyzeVideoRepository.save(orderAnalyzeVideo);

        OrderAnalyzeVideoResponse response =
                OrderAnalyzeVideoResponse.builder()
                        .orderId(orderAnalyzeVideo.getId())
                        .userId(userId)
                        .creditCost(costOfOrderVideoAnalyze)
                        .videoUrl(orderAnalyzeVideoDto.getVideoLink())
                        .videoGeminiUrl(video_s3_url)
                        .build();

        log.info("order send to rabbitmq for video analyze to user service");

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ANALYZE_VIDEO_EXCHANGE,
                RabbitMqConfig.ORDER_ANALYZE_VIDEO_ROUTING_KEY,
                response
        );

        return orderAnalyzeVideo.getId();
    }

    public CheckOrderAnalyzeVideoStatusResponse checkOrderAnalyzeVideoId(UUID userId, Long orderId) {

        OrderAnalyzeVideo order =
                orderAnalyzeVideoRepository.findById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        ));

        log.info(
                "received request to check order analyze video status for order info: {} ",
                order
        );

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this order"
            );
        }

        if (order.getStatus() == OrderStatus.SUCCEED) {
            log.info("order analyze video status is succeed");

            return CheckOrderAnalyzeVideoStatusResponse.builder()
                    .diagramId(order.getDiagramId())
                    .build();
        }

        else if (order.getStatus() == OrderStatus.SCRAPING_COMPLETED) {
            return CheckOrderAnalyzeVideoStatusResponse.builder()
                    .finishType(OrderAnalyzeVideoStatusType.FINISH_DOWNLOAD_VIDEO)
                    .build();
        }

        else if (order.getStatus() == OrderStatus.PURCHASED) {
            return CheckOrderAnalyzeVideoStatusResponse.builder()
                    .finishType(OrderAnalyzeVideoStatusType.PURCHASE)
                    .build();
        }

        else if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "You don't have enough credits to analyze the video"
            );
        }
        else if (order.getStatus() == OrderStatus.FAIL_TO_SCRAPE || order.getStatus() == OrderStatus.REFUND_COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "We couldn't process this video. Make sure the link is from a supported platform and the video is publicly accessible."
            );
        }
        else if (order.getStatus() == OrderStatus.SERVER_FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong on our server. Please try again later."
            );
        }

        return null;
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_VIDEO_STATUS_QUEUE)
    public void orderAnalyzeVideoStatus(OrderAnalyzeVideoStatusDto orderStatusDto) {


        if (orderStatusDto.getStatus() == null) {
            return;
        }

        OrderAnalyzeVideo order =
                orderAnalyzeVideoRepository.findById(orderStatusDto.getOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found"));

        switch (orderStatusDto.getStatus()) {

            case PURCHASED -> {
                int updated = orderAnalyzeVideoRepository.updateStatusIfCurrent(
                        orderStatusDto.getOrderId(),
                        OrderStatus.PENDING,
                        OrderStatus.PURCHASED,
                        Instant.now()
                );

                if (updated == 0) {
                    log.warn(
                            "Ignoring PURCHASED event for order {} because it is no longer PENDING",
                            orderStatusDto.getOrderId()
                    );
                    return;
                }

                sseOrderAnalyzeVideoService.sendStatus(
                        orderStatusDto.getOrderId(),
                        OrderAnalyzeVideoStatusResponse.builder()
                                .orderId(orderStatusDto.getOrderId())
                                .status(Status.SUCCEED)
                                .message("Purchased successfully")
                                .orderAnalyzeVideoStatusType(
                                        OrderAnalyzeVideoStatusType.PURCHASE
                                )
                                .build()
                );
            }

            case PAYMENT_FAILED -> {
                sseOrderAnalyzeVideoService.sendFinalStatus(
                        orderStatusDto.getOrderId(),
                        OrderAnalyzeVideoStatusResponse.builder()
                                .orderId(orderStatusDto.getOrderId())
                                .status(Status.PAYMENT_FAILED)
                                .message(
                                        "You don't have enough credits to complete this purchase."
                                )
                                .orderAnalyzeVideoStatusType(
                                        OrderAnalyzeVideoStatusType.PURCHASE
                                )
                                .build()
                );
            }
            default -> {
                sseOrderAnalyzeVideoService.sendFinalStatus(
                        orderStatusDto.getOrderId(),
                        OrderAnalyzeVideoStatusResponse.builder()
                                .orderId(orderStatusDto.getOrderId())
                                .status(Status.SERVER_FAILED)
                                .message(
                                        "Something went wrong while processing your video. Please check that the file or link you provided meets the requirements. If everything looks correct, please try again later."
                                )
                                .orderAnalyzeVideoStatusType(
                                        OrderAnalyzeVideoStatusType.PURCHASE
                                )
                                .build()
                );
            }
        }
    }

    @RabbitListener(queues = RabbitMqConfig.USER_REFUND_QUEUE)
    public void setRefundStatus(Long orderId) {

        OrderAnalyzeVideo order =
                orderAnalyzeVideoRepository.findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found by id: " + orderId
                                )
                        );

        order.setStatus(OrderStatus.REFUND_COMPLETED);

        orderAnalyzeVideoRepository.save(order);
    }

    @RabbitListener(queues = RabbitMqConfig.SCRAPING_FINISHED_QUEUE)
    public void getScrapingStatus(ScrapingCompleteResponse result) {
        log.info("scraping finished response received \n{}", result);
        if (result.getOrderId() == null) {
            log.error("order id is empty in scraping finished queue");
            return;
        }

        OrderAnalyzeVideo order =
                orderAnalyzeVideoRepository.findById(result.getOrderId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found in scraping finished queue"
                                )
                        );

        if (result.getStatus().equals(ScrapingStatus.FAILED)) {

            order.setStatus(OrderStatus.FAIL_TO_SCRAPE);
            orderAnalyzeVideoRepository.save(order);

            FailedAnalyzeVideoResponse response =
                    FailedAnalyzeVideoResponse.builder()
                            .userId(order.getUserId())
                            .credit(costOfOrderVideoAnalyze)
                            .orderId(order.getId())
                            .build();

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ANALYZE_VIDEO_EXCHANGE,
                    RabbitMqConfig.FAILED_ANALYZE_VIDEO_ROUTING_KEY,
                    response
            );

            sseOrderAnalyzeVideoService.sendFinalStatus(
                    result.getOrderId(),
                    OrderAnalyzeVideoStatusResponse.builder()
                            .orderId(result.getOrderId())
                            .status(Status.SERVER_FAILED)
                            .message(result.getMessage())
                            .orderAnalyzeVideoStatusType(
                                    OrderAnalyzeVideoStatusType.FINISH_DOWNLOAD_VIDEO
                            )
                            .build()
            );

            return;
        }

        order.setStatus(OrderStatus.SCRAPING_COMPLETED);
        orderAnalyzeVideoRepository.save(order);

        sseOrderAnalyzeVideoService.sendStatus(
                result.getOrderId(),
                OrderAnalyzeVideoStatusResponse.builder()
                        .orderId(result.getOrderId())
                        .status(Status.SUCCEED)
                        .message(result.getMessage())
                        .orderAnalyzeVideoStatusType(
                                OrderAnalyzeVideoStatusType.FINISH_DOWNLOAD_VIDEO
                        )
                        .build()
        );
    }

    @RabbitListener(queues = RabbitMqConfig.ANALYZE_VIDEO_QUEUE)
    public void analyzeVideoResponse(VideoAnalyzerDiagramResponse videoAnalyzerDiagramResponse) {
        log.info("analyze video response received \n{}", videoAnalyzerDiagramResponse);
        OrderAnalyzeVideo order =
                orderAnalyzeVideoRepository
                        .findById(videoAnalyzerDiagramResponse.getOrderId())
                        .orElse(null);

        if (order == null) {
            log.error("diagram response is empty in video analyze queue");
            return;
        }

        if (!videoAnalyzerDiagramResponse
                .getStatus()
                .equals(OrderStatus.SUCCEED)) {

            FailedAnalyzeVideoResponse response =
                    FailedAnalyzeVideoResponse.builder()
                            .userId(order.getUserId())
                            .credit(costOfOrderVideoAnalyze)
                            .orderId(order.getId())
                            .build();

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ANALYZE_VIDEO_EXCHANGE,
                    RabbitMqConfig.FAILED_ANALYZE_VIDEO_ROUTING_KEY,
                    response
            );

            order.setStatus(OrderStatus.SERVER_FAILED);

        } else {

            order.setStatus(OrderStatus.SUCCEED);
            order.setDiagramId(
                    videoAnalyzerDiagramResponse.getDiagramId()
            );

        }
        orderAnalyzeVideoRepository.save(order);
        sseOrderAnalyzeVideoService
                .sendFinalStatusDiagram(videoAnalyzerDiagramResponse);

        if (videoAnalyzerDiagramResponse.getVideoGeminiUrl() != null
                && !videoAnalyzerDiagramResponse
                .getVideoGeminiUrl()
                .isBlank()) {

            try {
                fileService.deleteFileFromGemini(
                        videoAnalyzerDiagramResponse.getVideoGeminiUrl()
                );
            } catch (Exception e) {
                log.error(
                        "Failed to delete Gemini file for order {}. URI: {}",
                        videoAnalyzerDiagramResponse.getOrderId(),
                        videoAnalyzerDiagramResponse.getVideoGeminiUrl(),
                        e
                );
            }
        }
    }
}