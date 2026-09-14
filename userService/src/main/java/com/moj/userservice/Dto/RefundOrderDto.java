package com.moj.userservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundOrderDto {
    private UUID userId;
    private Long credit;
    private Long orderId;
}
