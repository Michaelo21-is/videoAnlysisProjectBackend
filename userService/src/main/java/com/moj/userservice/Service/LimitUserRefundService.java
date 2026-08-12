package com.moj.userservice.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LimitUserRefundService {
    private static final String LIMIT_USER_REFUND_VIDEO_ANALYZE_PREFIX = "user:refund:analyze-video:";
    private static final Duration LIMIT_USER_REFUND_VIDEO_ANALYZE_DURATION = Duration.ofMinutes(1);

    private StringRedisTemplate redis;
    public LimitUserRefundService(StringRedisTemplate redis) {
        this.redis = redis;
    }
    public boolean tryRefundVideoAnalyze(Long orderId) {
        String key = LIMIT_USER_REFUND_VIDEO_ANALYZE_PREFIX + orderId;

        Boolean inserted = redis.opsForValue().setIfAbsent(
                key,
                "true",
                LIMIT_USER_REFUND_VIDEO_ANALYZE_DURATION
        );

        return Boolean.TRUE.equals(inserted);
    }
}
