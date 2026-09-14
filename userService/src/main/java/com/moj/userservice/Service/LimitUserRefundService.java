package com.moj.userservice.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LimitUserRefundService {
    public static final String LIMIT_USER_REFUND_VIDEO_ANALYZE_PREFIX = "user:refund:analyze-video:";
    public static final String LIMIT_USER_REFUND_ANALYZE_CONTENT_PREFIX = "user:refund:analyze-content:";
    private static final Duration LIMIT_USER_REFUND_VIDEO_ANALYZE_DURATION = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;
    public LimitUserRefundService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isRefundAlreadyBeingHandled(Long orderId, String prefix) {
        String key = prefix + orderId;

        Boolean inserted = redis.opsForValue().setIfAbsent(
                key,
                "true",
                LIMIT_USER_REFUND_VIDEO_ANALYZE_DURATION
        );

        return !Boolean.TRUE.equals(inserted);
    }
}
