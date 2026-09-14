package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.ScrapingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeContentResponseDto {
    private List<String> analyzeVideosDiagramIds;
    private List<String> createdVideosDiagramIds;
    private Long orderId;
    private ScrapingStatus status;
    private String message;
}
