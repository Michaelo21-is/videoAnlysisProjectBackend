package com.moj.notificationservice.Dto;

import com.moj.notificationservice.Enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderCreditStatusDto {
    private Long orderId;
    private Long creditAdded;
    private BigDecimal priceInUsd;
    private String email;
    private String fullName;
    private OrderStatus orderCreditStatus;
}
