package com.moj.userservice.Dto;

import com.moj.userservice.Enums.Platform;
import com.moj.userservice.Enums.SumOfContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAnalyzeContentDto {
    private Long orderId;
    private UUID userId;
    private Long creditCost;
    private SumOfContent sumOfContent;
    private Platform platform;
    private String niche;
}
