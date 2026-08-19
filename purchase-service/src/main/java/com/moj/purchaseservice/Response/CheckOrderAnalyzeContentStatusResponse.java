package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.OrderAnalyzeContentStatusResponse;
import com.moj.purchaseservice.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckOrderAnalyzeContentStatusResponse {
    private String message;
    private OrderAnalyzeContentStatusResponse status;
    private String niche;
    private Platform platform;
}
