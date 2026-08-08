package com.moj.userservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderAnalyzeVideoResponse {
    private Long orderId;
    private UUID userId;
    private String videoUrl;
    private String videoS3Url;
}
