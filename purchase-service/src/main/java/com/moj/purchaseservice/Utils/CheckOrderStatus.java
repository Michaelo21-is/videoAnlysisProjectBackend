package com.moj.purchaseservice.Utils;

import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Response.OrderAnalyzeStatusResponse;
import com.moj.purchaseservice.enums.Status;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CheckOrderStatus {

    public OrderAnalyzeStatusResponse createStatusResponse(
            OrderAnalyzeContents order
    ) {
        return switch (order.getStatus()) {
            case PURCHASED -> OrderAnalyzeStatusResponse.builder()
                    .orderId(order.getId())
                    .status(Status.SUCCEED)
                    .message("Order content analysis succeeded")
                    .build();

            case PAYMENT_FAILED -> OrderAnalyzeStatusResponse.builder()
                    .orderId(order.getId())
                    .status(Status.PAYMENT_FAILED)
                    .message("Not enough credits in your account")
                    .build();

            default -> OrderAnalyzeStatusResponse.builder()
                    .orderId(order.getId())
                    .status(Status.SERVER_FAILED)
                    .message("Something went wrong with our server. Please try again later.")
                    .build();
        };
    }
}
