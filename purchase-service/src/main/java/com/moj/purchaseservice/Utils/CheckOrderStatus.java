package com.moj.purchaseservice.Utils;

import com.moj.purchaseservice.Response.OrderResponse;
import com.moj.purchaseservice.enums.OrderStatus;
import com.moj.purchaseservice.enums.Status;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CheckOrderStatus {

    public OrderResponse createStatusResponse(Long orderId, OrderStatus orderStatus, String successMessage) {
        return switch (orderStatus) {
            case PURCHASED -> OrderResponse.builder()
                    .orderId(orderId)
                    .status(Status.SUCCEED)
                    .message(successMessage)
                    .build();

            case PAYMENT_FAILED -> OrderResponse.builder()
                    .orderId(orderId)
                    .status(Status.PAYMENT_FAILED)
                    .message("Not enough credits in your account")
                    .build();

            default -> OrderResponse.builder()
                    .orderId(orderId)
                    .status(Status.SERVER_FAILED)
                    .message(
                            "Something went wrong with our server. Please try again later."
                    )
                    .build();
        };
    }
}