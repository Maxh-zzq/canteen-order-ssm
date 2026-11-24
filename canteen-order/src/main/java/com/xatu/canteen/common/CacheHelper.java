package com.xatu.canteen.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存读写门面。Redis 异常时降级为「缓存未命中」，不影响主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheHelper {

    private final StringRedisTemplate redis;

    public <T> T get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : JsonUtil.parse(json, type);
        } catch (Exception e) {
            log.warn("读取缓存失败 key={}, cause={}", key, e.getMessage());
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : JsonUtil.parseList(json, type);
        } catch (Exception e) {
            log.warn("读取缓存失败 key={}, cause={}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        set(key, value, ttl, false);
    }

    /**
     * @param jitter 是否加随机抖动，防止同一时刻大面积失效（缓存雪崩）
     */
    public void set(String key, Object value, Duration ttl, boolean jitter) {
        try {
            Duration actual = ttl;
            if (jitter) {
                long seconds = Math.max(1, Math.min(300, ttl.getSeconds() / 10));
                actual = ttl.plusSeconds(ThreadLocalRandom.current().nextLong(0, seconds + 1));
            }
            redis.opsForValue().set(key, JsonUtil.toJson(value), actual);
        } catch (Exception e) {
            log.warn("写入缓存失败 key={}, cause={}", key, e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("删除缓存失败 key={}, cause={}", key, e.getMessage());
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key));
        } catch (Exception e) {
            log.warn("判断缓存失败 key={}, cause={}", key, e.getMessage());
            return false;
        }
    }
}
