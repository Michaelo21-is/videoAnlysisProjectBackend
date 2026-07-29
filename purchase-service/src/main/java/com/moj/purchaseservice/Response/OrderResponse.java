package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderResponse {
    private Status status;
    private String message;
    private Long orderId;
}
