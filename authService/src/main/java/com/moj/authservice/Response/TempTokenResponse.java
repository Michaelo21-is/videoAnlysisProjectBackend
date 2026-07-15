package com.moj.authservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempTokenResponse {
    private String message;
    private HttpStatus status;
    private String tempToken;
}
