package com.premtsd.linkedin.platform.persistence;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed {@link RecentWrites}: one key per user with a TTL equal to the sticky window.
 * {@code markWritten} SETs the key with expiry; {@code wroteRecently} is a key-existence check.
 * Shared across all app instances, so read-your-writes holds even when the follow-up read is
 * served by a different {@code web}/{@code worker} replica than the one that took the write.
 *
 * <p>Active under 'feeddb' — the only profile with a read/write split (and, in the compose
 * stack, a Redis to talk to). Set the window from the largest replication lag you tolerate.
 */
@Component
@Profile("feeddb")
class RedisRecentWrites implements RecentWrites {

    private final StringRedisTemplate redis;
    private final Duration window;

    RedisRecentWrites(StringRedisTemplate redis,
                      @Value("${app.ryow.window:5s}") Duration window) {
        this.redis = redis;
        this.window = window;
    }

    @Override
    public void markWritten(long userId) {
        redis.opsForValue().set(key(userId), "1", window);
    }

    @Override
    public boolean wroteRecently(long userId) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId)));
    }

    private static String key(long userId) {
        return "ryow:" + userId;
    }
}
