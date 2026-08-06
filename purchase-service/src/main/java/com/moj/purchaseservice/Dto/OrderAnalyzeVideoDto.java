package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderAnalyzeVideoDto {
    private MultipartFile videoFile;
    private String videoLink;
}
