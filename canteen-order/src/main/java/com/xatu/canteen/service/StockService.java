package com.xatu.canteen.service;

import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.entity.DailyMenu;
import com.xatu.canteen.mapper.DailyMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 库存服务。Redis 只做「预扣 + 快速失败」，数据库条件更新才是最终依据；
 * Redis 故障时直接降级为纯数据库扣减，不影响正确性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    /**
     * 返回值：>=0 扣减后剩余；-1 key 不存在；-2 库存不足。
     */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>(
            "local s = redis.call('get', KEYS[1]) "
                    + "if s == false then return -1 end "
                    + "local left = tonumber(s) "
                    + "local q = tonumber(ARGV[1]) "
                    + "if left < q then return -2 end "
                    + "redis.call('decrby', KEYS[1], q) "
                    + "return left - q",
            Long.class);

    private final StringRedisTemplate redis;
    private final DailyMenuMapper dailyMenuMapper;

    /**
     * 尝试预扣库存。Redis 异常返回 true，由数据库条件更新兜底。
     */
    public boolean tryDeduct(Long menuId, int quantity) {
        String key = RedisKeys.menuStock(menuId);
        try {
            Long result = redis.execute(DEDUCT_SCRIPT, Collections.singletonList(key), String.valueOf(quantity));
            if (result == null) {
                return true;
            }
            if (result == -1L) {
                int left = loadDbStock(menuId);
                if (left < quantity) {
                    return false;
                }
                resetStock(menuId, left);
                Long retry = redis.execute(DEDUCT_SCRIPT, Collections.singletonList(key), String.valueOf(quantity));
                return retry != null && retry >= 0;
            }
            return result >= 0;
        } catch (Exception e) {
            log.warn("Redis 预扣库存失败，降级为数据库扣减 menuId={}, cause={}", menuId, e.getMessage());
            return true;
        }
    }

    /**
     * 回滚 Redis 预扣。数据库部分由事务回滚，不需要在这里补偿。
     */
    public void restoreCache(Long menuId, int quantity) {
        String key = RedisKeys.menuStock(menuId);
        try {
            if (Boolean.TRUE.equals(redis.hasKey(key))) {
                redis.opsForValue().increment(key, quantity);
            }
        } catch (Exception e) {
            log.warn("回滚 Redis 库存失败 menuId={}, cause={}", menuId, e.getMessage());
        }
    }

    /**
     * 数据库库存回滚 + Redis 同步增加（取消、退款使用）。
     */
    public void restore(Long menuId, int quantity) {
        dailyMenuMapper.restoreStock(menuId, quantity);
        restoreCache(menuId, quantity);
    }

    public void initStock(Long menuId, int stockLeft) {
        try {
            redis.opsForValue().setIfAbsent(RedisKeys.menuStock(menuId), String.valueOf(Math.max(stockLeft, 0)), ttl());
        } catch (Exception e) {
            log.warn("初始化 Redis 库存失败 menuId={}, cause={}", menuId, e.getMessage());
        }
    }

    public void resetStock(Long menuId, int stockLeft) {
        try {
            redis.opsForValue().set(RedisKeys.menuStock(menuId), String.valueOf(Math.max(stockLeft, 0)), ttl());
        } catch (Exception e) {
            log.warn("重置 Redis 库存失败 menuId={}, cause={}", menuId, e.getMessage());
        }
    }

    /**
     * 读取当前库存：优先 Redis，失败回退数据库值。
     */
    public int currentStock(Long menuId, int dbStockLeft) {
        try {
            String value = redis.opsForValue().get(RedisKeys.menuStock(menuId));
            return value == null ? dbStockLeft : Integer.parseInt(value);
        } catch (Exception e) {
            return dbStockLeft;
        }
    }

    private int loadDbStock(Long menuId) {
        DailyMenu menu = dailyMenuMapper.selectById(menuId);
        return menu == null || menu.getStockLeft() == null ? 0 : menu.getStockLeft();
    }

    private Duration ttl() {
        Duration duration = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
                .plusMinutes(10);
        return duration.isNegative() || duration.isZero() ? Duration.ofMinutes(10) : duration;
    }
}
