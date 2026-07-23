package com.moj.authservice.Util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class ExtractIpFromClient {
    public static String extractClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(header -> header.split(",")[0].trim())
                .filter(ip -> !ip.isEmpty())
                .orElse(request.getRemoteAddr());
    }
}
