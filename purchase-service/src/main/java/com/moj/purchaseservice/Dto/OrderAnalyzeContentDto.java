package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.Platform;
import com.moj.purchaseservice.enums.SumOfContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderAnalyzeContentDto {
    private String niche;
    private SumOfContent sumOfContent;
    private Platform platform;
}
