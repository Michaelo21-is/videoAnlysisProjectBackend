package com.moj.userservice.Response;

import com.moj.userservice.Enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderAnalyzeContentStatusResponse {
    private Long orderId;
    private OrderStatus status;
}
