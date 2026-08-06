package com.moj.userservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAnalyzeVideoDto {
    private Long orderId;
    private Long creditCost;
    private UUID userId;
    private String videoUrl;
    private String videoS3Url;
}
