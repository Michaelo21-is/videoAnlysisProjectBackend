package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.OrderAnalyzeContentStatusResponse;
import com.moj.purchaseservice.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckOrderAnalyzeContentStatusResponse {
    private String message;
    private OrderAnalyzeContentStatusResponse status;
    private String niche;
    private Platform platform;
    private Integer totalContent;
    private List<VideoDetails> analyzedVideo;
    private List<VideoDetails> createdVideo;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class VideoDetails {
        private String diagramId;
        private String  videoName;
    }
}
