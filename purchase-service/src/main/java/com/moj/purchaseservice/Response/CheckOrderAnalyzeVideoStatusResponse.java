package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.OrderAnalyzeVideoStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckOrderAnalyzeVideoStatusResponse {
    private OrderAnalyzeVideoStatusType finishType;
    private String diagramId;
}
