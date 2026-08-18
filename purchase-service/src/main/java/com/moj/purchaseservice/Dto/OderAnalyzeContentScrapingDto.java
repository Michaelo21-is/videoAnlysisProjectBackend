package com.moj.purchaseservice.Dto;


import com.moj.purchaseservice.enums.ScrapingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OderAnalyzeContentScrapingDto {
    private Long orderId;
    private String message;
    private ScrapingStatus status;
    private ExtractedVideoDetails extractedVideoDetails;
}
