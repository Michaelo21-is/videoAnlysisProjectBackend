package com.moj.userservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailsResponse {
    private String productName;
    private String productDescription;
    private String productTargetAudience;
    private String s3ImageKey;
}
