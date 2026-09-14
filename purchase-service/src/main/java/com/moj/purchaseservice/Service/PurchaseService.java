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
    private final ContentPricingCalculator contentPricingCalculator;
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
            FileService fileService, ContentPricingCalculator contentPricingCalculator
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
        this.contentPricingCalculator = contentPricingCalculator;
    }

    /// ***
    /// order analyze content
    /// ***
    public Long orderAnalyzeContent(UUID userId, OrderAnalyzeContentDto orderAnalyzeContentDto) {

        Long creditCost = contentPricingCalculator.calculatePricingByNumOfContents(orderAnalyzeContentDto.getSumOfContent());

        OrderAnalyzeContents orderAnalyzeContents = OrderAnalyzeContents.builder()
                .userId(userId)
                .status(OrderAnalyzeContentStatus.PENDING)
                .sumOfContent(orderAnalyzeContentDto.getSumOfContent())
                .creditCost(creditCost)
                .niche(orderAnalyzeContentDto.getNiche())
                .platform(orderAnalyzeContentDto.getPlatform())
                .build();

        orderAnalyzeContentRepository.save(orderAnalyzeContents);

        OrderAnalyzeContentResponse response = OrderAnalyzeContentResponse.builder()
                .orderId(orderAnalyzeContents.getId())
                .creditCost(creditCost)
                .sumOfContent(orderAnalyzeContentDto.getSumOfContent())
                .platform(orderAnalyzeContentDto.getPlatform())
                .userId(userId)
                .niche(orderAnalyzeContentDto.getNiche())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_EXCHANGE,
                RabbitMqConfig.ORDER_ANALYZE_CONTENT_ROUTING_KEY,
                response
        );

        return orderAnalyzeContents.getId();
    }
    public CheckOrderAnalyzeContentStatusResponse checkOrderAnalyzeContentStatus(UUID userId ,Long orderId) {
        if (orderId == null || userId == null) {
            log.error("userId or orderId is null in check order analyze content status, order id: {}, user id: {}", orderId, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Didn't get the required details");
        }
        OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to access this order");
        }
        int sumOfVideos = 0;
        switch (order.getSumOfContent()){
            case THREE -> {
                sumOfVideos = 3;
            }
            case FIVE -> {
                sumOfVideos = 5;
            }
            case SEVEN -> {
                sumOfVideos = 7;
            }
        }

        switch (order.getStatus()) {
            case PURCHASED -> {
                return CheckOrderAnalyzeContentStatusResponse.builder()
                        .message("Order analyze content purchased successfully")
                        .status(OrderAnalyzeContentStatusResponse.PURCHASED)
                        .totalContent(sumOfVideos)
                        .niche(order.getNiche())
                        .platform(order.getPlatform())
                        .build();
            }
            case SCRAPING_COMPLETED -> {
                return CheckOrderAnalyzeContentStatusResponse.builder()
                        .message("Order analyze content scraping completed successfully")
                        .status(OrderAnalyzeContentStatusResponse.SCRAPING_COMPLETED)
                        .niche(order.getNiche())
                        .platform(order.getPlatform())
                        .totalContent(sumOfVideos)
                        .build();
            }
            case PICKUP_VIDEOS_COMPLETED -> {
                return CheckOrderAnalyzeContentStatusResponse.builder()
                        .message("Order analyze content scraping completed successfully")
                        .status(OrderAnalyzeContentStatusResponse.PICKUP_VIDEOS_COMPLETED)
                        .niche(order.getNiche())
                        .platform(order.getPlatform())
                        .totalContent(sumOfVideos)
                        .build();
            }
            case ANALYZE_CONTENT_COMPLETED -> {
                return CheckOrderAnalyzeContentStatusResponse.builder()
                        .message("Order analyze content scraping completed successfully")
                        .status(OrderAnalyzeContentStatusResponse.ANALYZING_VIDEOS_COMPLETED)
                        .niche(order.getNiche())
                        .platform(order.getPlatform())
                        .totalContent(sumOfVideos)
                        .build();
            }
            case SUCCEED -> {
                return CheckOrderAnalyzeContentStatusResponse.builder()
                        .message("complete analyze content successfully")
                        .status(OrderAnalyzeContentStatusResponse.GENERATING_RESPONSE)
                        .niche(order.getNiche())
                        .platform(order.getPlatform())
                        .totalContent(sumOfVideos)
                        .build();
            }
            default -> {
                return CheckOrderAnalyzeContentStatusResponse.builder()
                        .message("failed to analyze content please try again later.")
                        .status(OrderAnalyzeContentStatusResponse.FAILED)
                        .build();
            }
        }
    }
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_STATUS_QUEUE)
    public void analyzeContentStatus(OrderAnalyzeContentStatusDto orderAnalyzeContentStatusDto) {
        if (orderAnalyzeContentStatusDto.getStatus() == null || orderAnalyzeContentStatusDto.getOrderId() == null) {
            log.error("some of the parameters are null in order analyze content status queue,\n" +
                    " order id: {}, status: {}", orderAnalyzeContentStatusDto.getOrderId(), orderAnalyzeContentStatusDto.getStatus());
            return;
        }
        OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(orderAnalyzeContentStatusDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(orderAnalyzeContentStatusDto.getStatus());
        orderAnalyzeContentRepository.save(order);
        if (order.getStatus().equals(OrderAnalyzeContentStatus.PURCHASED)) {
            sseOrderAnalyzeContentService.sendPurchaseStatus(OrderResponse.builder()
                    .status(Status.SUCCEED)
                    .orderId(orderAnalyzeContentStatusDto.getOrderId())
                    .message("Order analyze content purchased successfully")
                    .build());
        }
        else{
            order.setStatus(OrderAnalyzeContentStatus.PAYMENT_FAILED);
            orderAnalyzeContentRepository.save(order);
            sseOrderAnalyzeContentService.sendFinalStatus(OrderResponse.builder()
                    .status(Status.PAYMENT_FAILED)
                    .orderId(orderAnalyzeContentStatusDto.getOrderId())
                    .message("Not enough credits in your account")
                    .build());
        }
    }
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_QUEUE)
    public void analyzeContentScrapeResponse(OderAnalyzeContentScrapingDto orderAnalyzeContentScrapingDto) {
        if (orderAnalyzeContentScrapingDto.getOrderId() == null) {
            log.error("order id is null in order analyze content scrape response queue");
            return;
        }
        OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(orderAnalyzeContentScrapingDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found in order analyze content scrape response queue"));
        if (orderAnalyzeContentScrapingDto.getStatus().equals(ScrapingStatus.FAILED)) {
            order.setStatus(OrderAnalyzeContentStatus.FAIL_TO_SCRAPE);
            orderAnalyzeContentRepository.save(order);
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDER_ANALYZE_CONTENT_EXCHANGE
                    , RabbitMqConfig.ORDER_ANALYZE_CONTENT_REFUND_REQUEST_ROUTING_KEY,
                    RefundResponse.builder()
                            .credit(order.getCreditCost())
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .build());
            sseOrderAnalyzeContentService.sendFinalStatus(OrderResponse.builder()
                    .status(Status.SERVER_FAILED)
                    .orderId(orderAnalyzeContentScrapingDto.getOrderId())
                    .message("Failed to scrape content, please try again later")
                    .build());
        }
        else{
            order.setStatus(OrderAnalyzeContentStatus.SCRAPING_COMPLETED);
            orderAnalyzeContentRepository.save(order);
            sseOrderAnalyzeContentService.sendScrapingStatus(orderAnalyzeContentScrapingDto);
        }
    }
    // after finishing analyzing content in llm service getting the diagram
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_RESPONSE_QUEUE)
    public void getAnalyzeContentResponse(AnalyzeContentResponseDto response) {
        if (response.getOrderId() == null) {
            log.error("order id is null in order analyze content response queue");
            return;
        }
        if(response.getCreatedVideosDiagramIds() == null){
            log.info("create video diagram id is null");
            sseOrderAnalyzeContentService.sendFinalStatus(OrderResponse.builder()
                    .status(Status.SERVER_FAILED)
                    .orderId(response.getOrderId())
                    .message("something went wrong please try refresh the page or try again later")
                    .build());
            return;
        }
        if (response.getStatus() == ScrapingStatus.FAILED) {
            OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(response.getOrderId())
            .orElseThrow(() -> new RuntimeException("Order not found in order analyze content response queue"));
            rabbitTemplate.convertAndSend(
            RabbitMqConfig.ORDER_ANALYZE_CONTENT_EXCHANGE
            , RabbitMqConfig.ORDER_ANALYZE_CONTENT_REFUND_REQUEST_ROUTING_KEY,
            RefundResponse.builder()
            .credit(order.getCreditCost())
            .orderId(order.getId())
            .userId(order.getUserId())
            .build());
            sseOrderAnalyzeContentService.sendFinalStatus(OrderResponse.builder()
                    .status(Status.SERVER_FAILED)
                    .orderId(response.getOrderId())
                    .message(response.getMessage())
                    .build());
            return;
        }
        // before sending for the user saving the video analyze diagram id and also the created video diagram id
        sseOrderAnalyzeContentService.sendAnalyzeContentFinalResponse(response);

    }
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_REFUND_STATUS_QUEUE)
    public void handleRefundAnalyzeContent(Long orderId) {
        OrderAnalyzeContents order = orderAnalyzeContentRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderAnalyzeContentStatus.REFUND_COMPLETED);
        orderAnalyzeContentRepository.save(order);
    }
    ///
    /// order analyze content
    ///

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

        log.info("received request to check order analyze video status for order info: {} ", order);

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
        if (!orderAnalyzeVideoRepository.existsById(orderStatusDto.getOrderId())){
            log.warn("order analyze video status is not found in order analyze video status queue order id: \n {} ", orderStatusDto.getOrderId());
        }

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

            RefundResponse response =
                    RefundResponse.builder()
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

            RefundResponse response =
                    RefundResponse.builder()
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