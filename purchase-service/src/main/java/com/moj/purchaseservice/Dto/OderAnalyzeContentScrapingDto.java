package com.moj.purchaseservice.Dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moj.purchaseservice.enums.ScrapingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OderAnalyzeContentScrapingDto {
    private Long orderId;
    private String message;
    private ScrapingStatus status;
    @JsonProperty("videos")
    private List<ExtractedVideoDetails> extractedVideoDetails;
}
