package com.moj.userservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderAnalyzeVideoResponse {
    private Long orderId;
    private String videoUrl;
    private String videoS3Url;
}
