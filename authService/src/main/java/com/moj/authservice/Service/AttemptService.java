package com.moj.authservice.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AttemptService {

    // Redis key prefixes
    private static final String LOGIN_FAILURE_KEY_PREFIX = "login:fail:";

    private static final String LOGIN_BLOCK_KEY_PREFIX = "login:block:";

    private static final String LOGIN_BLOCK_COUNT_KEY_PREFIX = "login:block-count:";

    // Maximum failed attempts before blocking
    private static final int ATTEMPT_LIMIT = 8;

    // How long failed attempts are kept
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    // First block duration
    private static final Duration BASE_BLOCK_TIME = Duration.ofMinutes(10);

    // Maximum block duration
    private static final Duration MAX_BLOCK_TIME = Duration.ofHours(24);

    // How long the previous block count is remembered
    private static final Duration BLOCK_COUNT_RETENTION = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    public AttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Tracks a failed login attempt.
     * Returns the block duration if this attempt caused
     */
    public Optional<Duration> trackFailedAttempt(String ip) {
        String failureKey = failureKey(ip);

        Long attempts = redis.opsForValue().increment(failureKey);

        if (attempts == null) {
            throw new IllegalStateException(
                    "Could not increment login attempts"
            );
        }

        /*
         * Set the expiration only on the first failed attempt.
         * This creates a fixed 15-minute attempt window.
         */
        if (attempts == 1) {
            redis.expire(
                    failureKey,
                    ATTEMPT_WINDOW
            );
        }

        /*
         * Block only when the counter reaches the exact limit.
         * This prevents increasing the block count more than once.
         */
        if (attempts == ATTEMPT_LIMIT) {
            Duration blockDuration = createBlock(ip);

            redis.delete(failureKey);

            return Optional.of(blockDuration);
        }

        return Optional.empty();
    }

    /**
     * Creates a block and increases the number
     * of previous blocks for this IP address.
     */
    private Duration createBlock(String ip) {
        String blockCountKey = blockCountKey(ip);

        Long blockCount = redis.opsForValue()
                .increment(blockCountKey);

        if (blockCount == null) {
            throw new IllegalStateException(
                    "Could not increment block count"
            );
        }

        /*
         * Keep the block history for seven days.
         * Every new block refreshes this expiration.
         */
        redis.expire(
                blockCountKey,
                BLOCK_COUNT_RETENTION
        );

        Duration blockDuration =
                calculateBlockDuration(blockCount);

        redis.opsForValue().set(
                blockKey(ip),
                "blocked",
                blockDuration
        );

        return blockDuration;
    }

    /**
     * Calculates the block duration according
     * to the number of previous blocks.
     * Block 1: 10 minutes
     * Block 2: 20 minutes
     */
    private Duration calculateBlockDuration(long blockCount) {
        int exponent = (int) Math.min(
                blockCount - 1,
                10
        );

        long multiplier = 1L << exponent;

        Duration calculated =
                BASE_BLOCK_TIME.multipliedBy(multiplier);

        if (calculated.compareTo(MAX_BLOCK_TIME) > 0) {
            return MAX_BLOCK_TIME;
        }

        return calculated;
    }

    /**
     * Checks whether the IP address is currently blocked and if yes returning the amount of time.
     * the ip is blocked
     */
    public Optional<Duration> getRemainingBlockTime(String ip) {
        Long seconds = redis.getExpire(
                blockKey(ip),
                TimeUnit.SECONDS
        );

        if (seconds == null || seconds <= 0) {
            return Optional.empty();
        }

        return Optional.of(Duration.ofSeconds(seconds));
    }

    /**
     * Clears the current failed attempts after a successful login.
     */
    public void clearFailedAttempts(String ip) {
        redis.delete(failureKey(ip));
    }

    private String failureKey(String ip) {
        return LOGIN_FAILURE_KEY_PREFIX + ip;
    }

    private String blockKey(String ip) {
        return LOGIN_BLOCK_KEY_PREFIX + ip;
    }

    private String blockCountKey(String ip) {
        return LOGIN_BLOCK_COUNT_KEY_PREFIX + ip;
    }
}