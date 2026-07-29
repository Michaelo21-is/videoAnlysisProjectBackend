package com.moj.userservice.Service;

import com.moj.userservice.Configuartion.RabbitMqConfig;
import com.moj.userservice.Dto.OrderAnalyzeDto;
import com.moj.userservice.Dto.OrderCreditDto;
import com.moj.userservice.Entity.Users;
import com.moj.userservice.Enums.AnalyzeOrderStatus;
import com.moj.userservice.Enums.OrderCreditStatus;
import com.moj.userservice.Repository.UserRepository;
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
    public UserService(UserRepository userRepository,  SendToQueue sendToQueue) {
        this.userRepository = userRepository;
        this.sendToQueue = sendToQueue;
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


    /// ***
    /// purchase area
    /// ***
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
            log.info("User not found");
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

        user.setCreditSum(updatedCreditSum);
        userRepository.save(user);

        sendToQueue.sendOrderStatus(
                orderAnalyzeDto.getOrderId(),
                AnalyzeOrderStatus.SUCCEED
        );
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREDIT_QUEUE)
    @Transactional
    public void handleOrderCredit(OrderCreditDto orderCreditDto) {
        if (orderCreditDto.getCredit() == null || orderCreditDto.getFullName() == null
                || orderCreditDto.getPriceInUsd() == null || orderCreditDto.getUserId() == null) {
            log.info("Order credit is null");
            sendToQueue.sendOrderCreditStatus( orderCreditDto, OrderCreditStatus.SERVER_FAILED);
            return;
        }
        Users user = userRepository.findById(orderCreditDto.getUserId())
                .orElse(null);
        if (user == null) {
            log.info("User not found");
            sendToQueue.sendOrderCreditStatus( orderCreditDto, OrderCreditStatus.SERVER_FAILED);
            return;
        }
        int updateRow = userRepository.addCredit(orderCreditDto.getUserId(), orderCreditDto.getCredit());
        if (updateRow == 0) {
            log.info("Failed to add credit");
            sendToQueue.sendOrderCreditStatus( orderCreditDto, OrderCreditStatus.FAILED_TO_ADD_CREDIT);
            return;
        }
        orderCreditDto.setEmail(user.getEmail());
        orderCreditDto.setFullName(user.getFullName());
        sendToQueue.sendOrderCreditStatus(orderCreditDto, OrderCreditStatus.SUCCEED);
    }

    /// ***
    /// purchase area
    /// ***
}
