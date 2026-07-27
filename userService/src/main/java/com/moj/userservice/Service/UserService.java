package com.moj.userservice.Service;

import com.moj.userservice.Configuartion.RabbitMqConfig;
import com.moj.userservice.Dto.OrderAnalyzeDto;
import com.moj.userservice.Entity.Users;
import com.moj.userservice.Enums.Status;
import com.moj.userservice.Repository.UserRepository;
import com.moj.userservice.Response.UserDetailsResponse;
import com.moj.userservice.Utils.SendToQueue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
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

            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto != null ? orderAnalyzeDto.getOrderId() : null,
                    orderAnalyzeDto != null ? orderAnalyzeDto.getUserId() : null,
                    Status.SERVER_FAILED
            );
            return;
        }

        Users user = userRepository.findById(orderAnalyzeDto.getUserId())
                .orElse(null);

        if (user == null) {
            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto.getOrderId(),
                    orderAnalyzeDto.getUserId(),
                    Status.SERVER_FAILED
            );
            return;
        }

        if (user.getCreditSum() == 0L || user.getCreditSum() < orderAnalyzeDto.getCreditCost()) {

            sendToQueue.sendOrderStatus(
                    orderAnalyzeDto.getOrderId(),
                    orderAnalyzeDto.getUserId(),
                    Status.PAYMENT_FAILED
            );
            return;
        }

        long updatedCreditSum = user.getCreditSum() - orderAnalyzeDto.getCreditCost();

        user.setCreditSum(updatedCreditSum);
        userRepository.save(user);

        sendToQueue.sendOrderStatus(
                orderAnalyzeDto.getOrderId(),
                orderAnalyzeDto.getUserId(),
                Status.SUCCEED
        );
    }


    /// ***
    /// purchase area
    /// ***
}
