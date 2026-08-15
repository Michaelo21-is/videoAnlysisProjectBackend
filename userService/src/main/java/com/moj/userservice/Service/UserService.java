package com.moj.userservice.Service;

import com.moj.userservice.Configuartion.RabbitMqConfig;
import com.moj.userservice.Dto.*;
import com.moj.userservice.Entity.BusinessContext;
import com.moj.userservice.Entity.Users;
import com.moj.userservice.Enums.AnalyzeOrderStatus;
import com.moj.userservice.Enums.OrderStatus;
import com.moj.userservice.Repository.BusinessContextRepository;
import com.moj.userservice.Repository.UserRepository;
import com.moj.userservice.Response.OrderAnalyzeVideoResponse;
import com.moj.userservice.Response.OrderVideoAnalysisStatusResponse;
import com.moj.userservice.Response.UserDetailsResponse;
import com.moj.userservice.Utils.SendToQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SendToQueue sendToQueue;
    private final LimitUserRefundService limitUserRefundService;
    private final BusinessContextRepository businessContextRepository;
    public UserService(UserRepository userRepository,  SendToQueue sendToQueue,
       LimitUserRefundService limitUserRefundService, BusinessContextRepository businessContextRepository) {
        this.userRepository = userRepository;
        this.sendToQueue = sendToQueue;
        this.limitUserRefundService = limitUserRefundService;
        this.businessContextRepository = businessContextRepository;
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
    /// analyze content
    ///
    @Transactional
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_CONTENT_QUEUE)
    public void handleOrderAnalyzeContent(OrderAnalyzeDto orderAnalyzeDto) {

        if (orderAnalyzeDto == null || orderAnalyzeDto.getUserId() == null || orderAnalyzeDto.getCreditCost() == null) {
            log.info("Order analyze content is null");
            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto != null ? orderAnalyzeDto.getOrderId() : null,
                    AnalyzeOrderStatus.SERVER_FAILED
            );
            return;
        }

        Users user = userRepository.findById(orderAnalyzeDto.getUserId())
                .orElse(null);

        if (user == null) {
            log.info("User not found in handle order analyze content");
            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto.getOrderId(),
                    AnalyzeOrderStatus.SERVER_FAILED
            );
            return;
        }

        if (user.getCreditSum() == 0L || user.getCreditSum() < orderAnalyzeDto.getCreditCost()) {
            log.info("User has no enough credit");
            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto.getOrderId(),
                    AnalyzeOrderStatus.PAYMENT_FAILED
            );
            return;
        }

        long updatedCreditSum = user.getCreditSum() - orderAnalyzeDto.getCreditCost();
        if (updatedCreditSum < 0) {
            log.info("Credit sum is less than 0");
            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto.getOrderId(),
                    AnalyzeOrderStatus.PAYMENT_FAILED
            );
        }
        user.setCreditSum(updatedCreditSum);
        userRepository.save(user);

        sendToQueue.sendOrderStatus(
                orderAnalyzeDto.getOrderId(),
                AnalyzeOrderStatus.SUCCEED
        );
    }
    /// ***
    /// analyze content
    /// ***

    /// ***
    /// order credit
    /// ***
    @RabbitListener(queues = RabbitMqConfig.ORDER_CREDIT_QUEUE)
    @Transactional
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
    /// order analyze video
    /// ***
    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYZE_VIDEO_QUEUE)
    public void handleOrderAnalyzeVideo(OrderAnalyzeVideoDto orderAnalyzeVideoDto) {
        log.info("reccived order deatils, info: {}", orderAnalyzeVideoDto);
        if (orderAnalyzeVideoDto.getUserId() == null || orderAnalyzeVideoDto.getCreditCost() == null || orderAnalyzeVideoDto.getOrderId() == null) {
            log.info("some of the parameters are null in handle order analyze video: \n {}", orderAnalyzeVideoDto);
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderStatus.SERVER_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        Users user = userRepository.findById(orderAnalyzeVideoDto.getUserId())
                .orElse(null);
        if (user == null) {
            log.info("User not found with the id");
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderStatus.SERVER_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        if (user.getCreditSum() == 0L || user.getCreditSum() < orderAnalyzeVideoDto.getCreditCost()) {
            log.info("something went wrong in purchase should not pass 0 credit or less than credit cost");
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderStatus.PAYMENT_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        long updatedCreditSum = user.getCreditSum() - orderAnalyzeVideoDto.getCreditCost();
        if (updatedCreditSum < 0) {
            log.info("not enough credit to purchase analyze video");
            OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                    .status(OrderStatus.PAYMENT_FAILED)
                    .build();
            sendToQueue.sendOrderAnalyzeStatus(response);
            return;
        }
        user.setCreditSum(updatedCreditSum);
        userRepository.save(user);
        OrderVideoAnalysisStatusResponse response = OrderVideoAnalysisStatusResponse.builder()
                .status(OrderStatus.PURCHASED)
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
    public void handleFailedAnalyzeVideo(FailedAnalyzeVideoDto failedAnalyzeVideoDto) {
        if (failedAnalyzeVideoDto.getCredit() == null || failedAnalyzeVideoDto.getUserId() == null || failedAnalyzeVideoDto.getOrderId() ==null) {
            log.info("some of the parameters are null: {}", failedAnalyzeVideoDto);
            return;
        }
        if (!limitUserRefundService.tryRefundVideoAnalyze(failedAnalyzeVideoDto.getOrderId())) {
            log.info(
                    "Refund already handled for order id: {}",
                    failedAnalyzeVideoDto.getOrderId()
            );
            return;
        }
        int addCredit = userRepository.addCredit(failedAnalyzeVideoDto.getUserId(), failedAnalyzeVideoDto.getCredit());
        if (addCredit == 0) {
            log.info("Failed to add credit to user with id: {}", failedAnalyzeVideoDto.getUserId());
            return;
        }
        sendToQueue.sendUserRefund(failedAnalyzeVideoDto.getOrderId());
    }
    /// ***
    /// order analyze video
    /// ***

    /// ***
    /// purchase area
    /// ***
}
