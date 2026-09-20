package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.OrderAnalyzeContentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAnalyzeContentStatusDto {
    private Long orderId;
    private OrderAnalyzeContentStatus status;
    private String businessContext;
    private String targetAudience;
}
