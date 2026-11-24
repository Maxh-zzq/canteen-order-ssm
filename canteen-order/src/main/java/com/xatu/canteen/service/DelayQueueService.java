package com.xatu.canteen.service;

import com.xatu.canteen.common.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 订单超时延迟队列。用 Redis ZSet 实现：score = 到期时间戳，定时任务轮询并原子 ZREM 抢占。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueService {

    private final StringRedisTemplate redis;

    public void add(String orderNo, LocalDateTime expireAt) {
        try {
            long score = expireAt.atZone(ZoneId.systemDefault()).toEpochSecond();
            redis.opsForZSet().add(RedisKeys.orderDelayQueue(), orderNo, score);
        } catch (Exception e) {
            log.error("写入订单延迟队列失败 orderNo={}, cause={}", orderNo, e.getMessage());
        }
    }

    public void remove(String orderNo) {
        try {
            redis.opsForZSet().remove(RedisKeys.orderDelayQueue(), orderNo);
        } catch (Exception e) {
            log.warn("移除订单延迟任务失败 orderNo={}, cause={}", orderNo, e.getMessage());
        }
    }

    /**
     * 取出已到期订单，只有成功 ZREM 的实例才能处理，避免重复消费。
     */
    public Set<String> pollDue(int limit) {
        try {
            long now = System.currentTimeMillis() / 1000;
            Set<String> due = redis.opsForZSet()
                    .rangeByScore(RedisKeys.orderDelayQueue(), 0, now, 0, limit);
            if (due == null || due.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> owned = new HashSet<>();
            for (String orderNo : due) {
                Long removed = redis.opsForZSet().remove(RedisKeys.orderDelayQueue(), orderNo);
                if (removed != null && removed > 0) {
                    owned.add(orderNo);
                }
            }
            return owned;
        } catch (Exception e) {
            log.warn("轮询订单延迟队列失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}
