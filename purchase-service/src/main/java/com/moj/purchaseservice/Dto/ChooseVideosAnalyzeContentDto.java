package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.Platform;
import com.moj.purchaseservice.enums.SumOfContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChooseVideosAnalyzeContentDto {
    private Long orderId;
    private UUID userId;
    private String productName;
    private String productDescription;
    private String productTargetAudience;
    private SumOfContent sumOfContent;
    private List<VideoDetails> videos;
    private Platform platform;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VideoDetails {
        private String videoUrl;
        private String mp4Url;
        private String videoName;
        private Boolean shouldSaveDiagram;
    }
}
