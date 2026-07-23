package com.moj.authservice.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ResetPasswordAttemptService {
    // Maximum failed attempts before blocking
    private static final int ATTEMPT_LIMIT = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofHours(1);
    private static final Duration BLOCK_DURATION = Duration.ofHours(6);

    private static final String RESET_PASSWORD_ATTEMPT_KEY_PREFIX = "reset-password:attempt:";
    private static final String RESET_PASSWORD_BLOCK_KEY_PREFIX = "reset-password:block:";
    private final StringRedisTemplate redis;
    public ResetPasswordAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }
    public Optional<Duration> trackResetAttempt(String ip){
        String resetPasswordAttemptKey = getResetPasswordAttemptKey(ip);
        Long attempts = redis.opsForValue().increment(resetPasswordAttemptKey);
        if (attempts == null) {
            throw new IllegalStateException(
                    "Could not increment login attempts"
            );
        }
        if (attempts == 1) {redis.expire(resetPasswordAttemptKey, ATTEMPT_WINDOW);}
        if (attempts == ATTEMPT_LIMIT) {
            createBlock(ip);
            redis.delete(resetPasswordAttemptKey);
            return Optional.of(BLOCK_DURATION);
        }
        return Optional.empty();
    }
    public Optional<Duration> getRemainingBlockTime(String ip){
        String resetPasswordBlockKey = getResetPasswordBlockKey(ip);
        Long seconds = redis.getExpire(resetPasswordBlockKey, java.util.concurrent.TimeUnit.SECONDS);
        if (seconds == null || seconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(seconds));
    }
    private void createBlock(String ip){
        String resetPasswordBlockKey = getResetPasswordBlockKey(ip);
        Long blockCount = redis.opsForValue().increment(resetPasswordBlockKey);
        if (blockCount == null) {
            throw new IllegalStateException(
                    "Could not increment block count"
            );
        }
        redis.expire(resetPasswordBlockKey, BLOCK_DURATION);
    }


    private String getResetPasswordAttemptKey(String ip) {
        return RESET_PASSWORD_ATTEMPT_KEY_PREFIX + ip;
    }
    private String getResetPasswordBlockKey(String ip) {
        return RESET_PASSWORD_BLOCK_KEY_PREFIX + ip;
    }

}
