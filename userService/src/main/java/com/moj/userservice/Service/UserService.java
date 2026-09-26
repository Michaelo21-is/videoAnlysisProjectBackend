package com.moj.userservice.Service;

import com.moj.userservice.Configuartion.RabbitMqConfig;
import com.moj.userservice.Dto.*;
import com.moj.userservice.Entity.BusinessContext;
import com.moj.userservice.Entity.BusinessProducts;
import com.moj.userservice.Entity.Users;
import com.moj.userservice.Enums.OrderAnalyzeContentStatus;
import com.moj.userservice.Enums.OrderStatus;
import com.moj.userservice.Repository.BusinessContextRepository;
import com.moj.userservice.Repository.BusinessProductsRepository;
import com.moj.userservice.Repository.UserRepository;
import com.moj.userservice.Response.*;
import com.moj.userservice.Utils.SendToQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SendToQueue sendToQueue;
    private final LimitUserRefundService limitUserRefundService;
    private final BusinessContextRepository businessContextRepository;
    private final S3AwsService s3AwsService;
    private final BusinessProductsRepository businessProductsRepository;
    @Value("${aws.s3.base-url}")
    private String baseUrl;
    public UserService(UserRepository userRepository,  SendToQueue sendToQueue, S3AwsService s3AwsService,
       LimitUserRefundService limitUserRefundService, BusinessContextRepository businessContextRepository, BusinessProductsRepository businessProductsRepository) {
        this.userRepository = userRepository;
        this.sendToQueue = sendToQueue;
        this.s3AwsService = s3AwsService;
        this.limitUserRefundService = limitUserRefundService;
        this.businessContextRepository = businessContextRepository;
        this.businessProductsRepository = businessProductsRepository;
    }
    public UserDetailsResponse getUserDetails(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID is missing");
        }
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User ID not found"
                ));

        return UserDetailsResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .creditSum(user.getCreditSum())
                .build();
    }
    public void setBusinessDetails(UUID userId, BusinessDetailsDto businessDetailsDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (businessDetailsDto.getBusinessName() == null || businessDetailsDto.getBusinessName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business name is missing");
        }

        if (businessDetailsDto.getNiche() == null || businessDetailsDto.getNiche().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niche is missing");
        }
        BusinessContext businessContext = BusinessContext.builder()
                .users(user)
                .businessName(businessDetailsDto.getBusinessName())
                .niche(businessDetailsDto.getNiche())
                .description(businessDetailsDto.getDescription())
                .targetAudience(businessDetailsDto.getTargetAudience())
                .build();
        businessContextRepository.save(businessContext);
    }

    public BusinessDetailsDto getBusinessDetails(UUID userId){
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        BusinessContext businessContext = businessContextRepository.findByUsers_Id(userId)
                .orElse(null);
        if (businessContext == null) {
            return BusinessDetailsDto.builder().build();
        }
        return BusinessDetailsDto.builder()
                .businessName(businessContext.getBusinessName())
                .niche(businessContext.getNiche())
                .description(businessContext.getDescription())
                .targetAudience(businessContext.getTargetAudience())
                .build();
    }
    public void updateBusinessDetails(UUID userId, BusinessDetailsDto businessDetailsDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        BusinessContext businessContext = businessContextRepository
                .findByUsers_Id(userId)
                .orElseGet(() -> BusinessContext.builder()
                        .users(user)
                        .build()
                );


        if (businessDetailsDto.getBusinessName() != null && !businessDetailsDto.getBusinessName().isBlank()) {
            businessContext.setBusinessName(businessDetailsDto.getBusinessName());
        }
        if (businessDetailsDto.getNiche() != null && !businessDetailsDto.getNiche().isBlank()) {
            businessContext.setNiche(businessDetailsDto.getNiche());
        }
        if (businessDetailsDto.getDescription() != null && !businessDetailsDto.getDescription().isBlank()) {
            businessContext.setDescription(businessDetailsDto.getDescription());
        }
        if (businessDetailsDto.getTargetAudience() != null && !businessDetailsDto.getTargetAudience().isBlank()) {
            businessContext.setTargetAudience(businessDetailsDto.getTargetAudience());
        }
        businessContextRepository.save(businessContext);
    }

    /// ***
    /// purchase area
    /// ***

    ///
    /// order analyze content
    ///
    @Transactional
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_QUEUE)
    public void handleOrderAnalyzeContent(OrderAnalyzeContentDto orderAnalyzeContentDto) {

        if (orderAnalyzeContentDto.getUserId() == null || orderAnalyzeContentDto.getCreditCost() == null || orderAnalyzeContentDto.getOrderId() == null) {
            log.info("check the parameters:  userId: {}, creditCost: {}, orderId: {}", orderAnalyzeContentDto.getUserId(), orderAnalyzeContentDto.getCreditCost(), orderAnalyzeContentDto.getOrderId());
            sendToQueue.sendOrderAnalyzeContentStatus(
                    OrderAnalyzeContentStatusResponse.builder()
                            .orderId(orderAnalyzeContentDto.getOrderId())
                            .status(OrderStatus.PAYMENT_FAILED)
                            .build()
            );
            return;
        }

        Users user = userRepository.findById(orderAnalyzeContentDto.getUserId())
                .orElse(null);
        BusinessContext business = businessContextRepository.findByUsers_Id(orderAnalyzeContentDto.getUserId())
                .orElse(null);
        if (user == null || business == null) {
            log.info("user or business not found the user:{}, business: {}, user id: {}", user, business, orderAnalyzeContentDto.getUserId());
            sendToQueue.sendOrderAnalyzeContentStatus(
                    OrderAnalyzeContentStatusResponse.builder()
                            .orderId(orderAnalyzeContentDto.getOrderId())
                            .status(OrderStatus.PAYMENT_FAILED)
                            .build()
            );
            return;
        }

        if (user.getCreditSum() == 0L || user.getCreditSum() < orderAnalyzeContentDto.getCreditCost()) {
            log.info("User has no enough credit");
            sendToQueue.sendOrderAnalyzeContentStatus(
                    OrderAnalyzeContentStatusResponse.builder()
                            .orderId(orderAnalyzeContentDto.getOrderId())
                            .status(OrderStatus.PAYMENT_FAILED)
                            .build()
            );
            return;
        }

        long updatedCreditSum = user.getCreditSum() - orderAnalyzeContentDto.getCreditCost();
        if (updatedCreditSum < 0) {
            log.info("Credit sum is less than 0");
            sendToQueue.sendOrderAnalyzeContentStatus(
                OrderAnalyzeContentStatusResponse.builder()
                .orderId(orderAnalyzeContentDto.getOrderId())
                .status(OrderStatus.PAYMENT_FAILED)
                .build()
            );
            return;
        }
        user.setCreditSum(updatedCreditSum);
        userRepository.save(user);

        sendToQueue.sendOrderAnalyzeContentStatus(OrderAnalyzeContentStatusResponse.builder()
                .orderId(orderAnalyzeContentDto.getOrderId())
                .status(OrderStatus.PURCHASED)
                .businessContext(business.getDescription())
                .targetAudience(business.getTargetAudience())
                .build());

        sendToQueue.sendOrderAnalyzeContentToScrape(OrderAnalyzeContentScrapeResponse.builder()
                .sumOfContent(orderAnalyzeContentDto.getSumOfContent())
                .orderId(orderAnalyzeContentDto.getOrderId())
                .platform(orderAnalyzeContentDto.getPlatform())
                .niche(orderAnalyzeContentDto.getNiche())
                .build());
    }
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_REFUND_REQUEST_QUEUE)
    public void refundOderAnalyzeContent(RefundOrderDto refundOrderDto) {
        if (refundOrderDto.getCredit() == null || refundOrderDto.getUserId() == null || refundOrderDto.getOrderId() ==null) {
            log.info("some of the parameters are null: {}", refundOrderDto);
            return;
        }
        if (limitUserRefundService.isRefundAlreadyBeingHandled(refundOrderDto.getOrderId(), LimitUserRefundService.LIMIT_USER_REFUND_ANALYZE_CONTENT_PREFIX)) {
            log.info("Refund already handled for order id: {}", refundOrderDto.getOrderId());
            return;
        }
        int addCredit = userRepository.addCredit(refundOrderDto.getUserId(), refundOrderDto.getCredit());
        if (addCredit == 0) {
            log.info("Failed to add credit to user with id: {}", refundOrderDto.getUserId());
        }
        sendToQueue.sendOrderAnalyzeContentRefundStatus(refundOrderDto.getOrderId());
    }
    /// ***
    /// analyze content
    /// ***

    /// ***
    /// order credit
    /// ***
    @Transactional
    @RabbitListener(queues = RabbitMqConfig.ORDER_CREDIT_QUEUE)
    public void handleOrderCredit(OrderCreditDto orderCreditDto) {
        if (orderCreditDto.getCredit() == null || orderCreditDto.getPriceInUsd() == null || orderCreditDto.getUserId() == null) {
            sendToQueue.sendOrderCreditStatus( orderCreditDto, OrderStatus.SERVER_FAILED);
            return;
        }
        Users user = userRepository.findById(orderCreditDto.getUserId())
                .orElse(null);
        if (user == null) {
            log.info("User not found");
            sendToQueue.sendOrderCreditStatus( orderCreditDto, OrderStatus.SERVER_FAILED);
            return;
        }
        int updateRow = userRepository.addCredit(orderCreditDto.getUserId(), orderCreditDto.getCredit());
        if (updateRow == 0) {
            log.info("Failed to add credit");
            sendToQueue.sendOrderCreditStatus( orderCreditDto, OrderStatus.FAILED_TO_ADD_CREDIT);
            return;
        }
        orderCreditDto.setEmail(user.getEmail());
        orderCreditDto.setFullName(user.getFullName());
        log.info("Credit added");
        sendToQueue.sendOrderCreditStatus(orderCreditDto, OrderStatus.SUCCEED);
    }

    /// ***
    /// order credit
    /// ***

    /// ***
    /// order to analyze video
    /// ***
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_VIDEO_QUEUE)
    public void handleOrderAnalyzeVideo(OrderAnalyzeVideoDto orderAnalyzeVideoDto) {
        log.info("received order details, info: {}", orderAnalyzeVideoDto);
        if (orderAnalyzeVideoDto.getUserId() == null || orderAnalyzeVideoDto.getCreditCost() == null || orderAnalyzeVideoDto.getOrderId() == null) {
            log.info("some of the parameters are null in handle order analyze video: \n {}", orderAnalyzeVideoDto);
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderAnalyzeContentStatus.SERVER_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        Users user = userRepository.findById(orderAnalyzeVideoDto.getUserId())
                .orElse(null);
        if (user == null) {
            log.info("User not found with the id");
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderAnalyzeContentStatus.SERVER_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        if (user.getCreditSum() == 0L || user.getCreditSum() < orderAnalyzeVideoDto.getCreditCost()) {
            log.info("something went wrong in purchase should not pass 0 credit or less than credit cost");
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderAnalyzeContentStatus.PAYMENT_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        long updatedCreditSum = user.getCreditSum() - orderAnalyzeVideoDto.getCreditCost();
        if (updatedCreditSum < 0) {
            log.info("not enough credit to purchase analyze video");
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderAnalyzeContentStatus.PAYMENT_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        user.setCreditSum(updatedCreditSum);
        userRepository.save(user);
        OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                .status(OrderAnalyzeContentStatus.PURCHASED)
                .orderId(orderAnalyzeVideoDto.getOrderId())
                .build();
        sendToQueue.sendOrderAnalyzeStatus(response);
        BusinessContext businessContext = businessContextRepository.findByUsers_Id(orderAnalyzeVideoDto.getUserId())
                .orElse(null);
        OrderAnalyzeVideoResponse orderAnalyzeVideoResponse = OrderAnalyzeVideoResponse.builder()
                .videoGeminiUrl(orderAnalyzeVideoDto.getVideoGeminiUrl())
                .videoUrl(orderAnalyzeVideoDto.getVideoUrl())
                .orderId(orderAnalyzeVideoDto.getOrderId())
                .userId(orderAnalyzeVideoDto.getUserId())
                .businessContext(businessContext != null ? businessContext.getDescription() : null)
                .businessTargetAudience(businessContext != null ? businessContext.getTargetAudience() : null)
                .build();
        sendToQueue.sendOrderToAnalyzeVideo(orderAnalyzeVideoResponse);
    }
    @RabbitListener(queues = RabbitMqConfig.FAILED_ANALYZE_VIDEO_QUEUE)
    public void handleRefundAnalyzeVideo(RefundOrderDto refundOrderDto) {
        if (refundOrderDto.getCredit() == null || refundOrderDto.getUserId() == null || refundOrderDto.getOrderId() ==null) {
            log.info("some of the parameters are null: {}", refundOrderDto);
            return;
        }
        if (limitUserRefundService.isRefundAlreadyBeingHandled(refundOrderDto.getOrderId(), LimitUserRefundService.LIMIT_USER_REFUND_VIDEO_ANALYZE_PREFIX)) {
            log.info(
                    "Refund already handled for order id: {}",
                    refundOrderDto.getOrderId()
            );
            return;
        }
        int addCredit = userRepository.addCredit(refundOrderDto.getUserId(), refundOrderDto.getCredit());
        if (addCredit == 0) {
            log.info("Failed to add credit to user with id: {}", refundOrderDto.getUserId());
            return;
        }
        sendToQueue.sendUserRefund(refundOrderDto.getOrderId());
    }
    /// ***
    /// order analyze video
    /// ***

    /// ***
    /// purchase area
    /// ***

    /// ***
    /// product area
    /// ***
    public void addProduct(MultipartFile file, AddProductDto addProductDto, UUID userId) {
        String key;
        try {
            key = s3AwsService.uploadFile(file);
        }
        catch (Exception e) {
            log.error("Failed to upload product image error message \n: {}", e.getMessage());
            return;
        }
        BusinessProducts products = BusinessProducts.builder()
                .productName(addProductDto.getProductName())
                .productDescription(addProductDto.getProductDescription())
                .s3ImageKey(key)
                .productTargetAudience(addProductDto.getProductTargetAudience())
                .users(userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .build();
        businessProductsRepository.save(products);
    }
    public Page<ProductDetailsResponse> getProductDetailsResponseForProfilePage(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<BusinessProducts> products = businessProductsRepository.findAllByUsers_Id(userId, pageable);

        return products.map(product -> ProductDetailsResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .productDescription(product.getProductDescription())
                .productTargetAudience(product.getProductTargetAudience())
                .s3Url(baseUrl + "/" +product.getS3ImageKey())
                .build());
    }
    public void deleteProduct(UUID userId, Long productId) {
        BusinessProducts products = businessProductsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!products.getUsers().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this product");
        }
        businessProductsRepository.deleteById(productId);
    }
}
