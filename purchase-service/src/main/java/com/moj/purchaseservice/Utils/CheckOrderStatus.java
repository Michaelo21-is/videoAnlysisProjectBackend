package com.moj.purchaseservice.Utils;

import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Response.OrderAnalyzeStatusResponse;
import com.moj.purchaseservice.enums.Status;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class CheckOrderStatus {
    private OrderAnalyzeStatusResponse createStatusResponse(OrderAnalyzeContents order) {
        return switch (order.getStatus()) {
            case PURCHASED -> OrderAnalyzeStatusResponse.builder()
                    .orderId(order.getId())
                    .status(Status.SUCCEED)
                    .message("Order analyze content succeeded")
                    .build();

            default -> OrderAnalyzeStatusResponse.builder()
                    .orderId(order.getId())
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .message("Order analyze content have been failed check your credit balance")
                    .build();
        };
    }
}
