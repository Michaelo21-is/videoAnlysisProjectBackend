package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.OrderAnalyzeContentStatus;
import com.moj.purchaseservice.enums.OrderStatus;
import com.moj.purchaseservice.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAnalyzeContentStatusDto {
    private Long orderId;
    private OrderAnalyzeContentStatus status;
}
