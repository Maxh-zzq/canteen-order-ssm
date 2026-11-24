package com.xatu.canteen.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis SETNX 的简易分布式锁。释放时用 Lua 校验持有者，避免误删别人的锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    /**
     * @return 加锁成功返回 token，失败或 Redis 异常返回 null
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean success = redis.opsForValue().setIfAbsent(key, token, ttl);
            return Boolean.TRUE.equals(success) ? token : null;
        } catch (Exception e) {
            log.warn("获取分布式锁失败 key={}, cause={}", key, e.getMessage());
            return null;
        }
    }

    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        try {
            redis.execute(RELEASE_SCRIPT, Collections.singletonList(key), token);
        } catch (Exception e) {
            log.warn("释放分布式锁失败 key={}, cause={}", key, e.getMessage());
        }
    }
}
