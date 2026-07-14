package com.moj.authservice.Response;

import com.moj.authservice.Enums.TwoFactorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TwoFactorResponse {
    private Integer verificationCode;
    private String email;
    private TwoFactorType twoFactorType;
}
