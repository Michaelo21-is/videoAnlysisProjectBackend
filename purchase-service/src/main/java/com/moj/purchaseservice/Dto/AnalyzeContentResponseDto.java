package com.moj.purchaseservice.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moj.purchaseservice.enums.ScrapingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeContentResponseDto {

    private List<VideoDetails> analyzedVideos;
    private List<VideoDetails> createdVideos;

    private Long orderId;
    private ScrapingStatus status;
    private String message;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VideoDetails {
        private String diagramId;
        private String videoName;

        // read from the LLM service message only, never sent back to the client over SSE
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String prompt;
    }
}
