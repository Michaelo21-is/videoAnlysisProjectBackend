package com.moj.userservice.Response;

import com.moj.userservice.Enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderCreditResponse {
    private Long orderId;
    private Long creditAdded;
    private BigDecimal priceInUsd;
    private String email;
    private String fullName;
    private OrderStatus orderStatus;
}
