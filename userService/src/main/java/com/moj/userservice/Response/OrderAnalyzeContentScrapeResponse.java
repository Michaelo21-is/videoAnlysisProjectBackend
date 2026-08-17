package com.moj.userservice.Response;

import com.moj.userservice.Enums.Platform;
import com.moj.userservice.Enums.SumOfContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderAnalyzeContentScrapeResponse {
    private Long orderId;
    private SumOfContent sumOfContent;
    private Platform platform;
    private String niche;
}
