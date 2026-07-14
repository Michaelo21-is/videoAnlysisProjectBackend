package com.moj.notificationservice.Dto;

import com.moj.notificationservice.Enums.TwoFactorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorEmailDataDto {
    private String email;
    private Integer verificationCode;
    private TwoFactorType twoFactorType;
}
