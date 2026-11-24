package com.xatu.canteen.common;

import com.xatu.canteen.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 限流切面。Redis 不可用时放行，避免因缓存故障导致业务全阻。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = StringUtils.hasText(rateLimit.key()) ? rateLimit.key() : point.getSignature().getName();
        String identity = resolveIdentity();
        String redisKey = RedisKeys.rateLimit(key, identity);
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, rateLimit.seconds(), TimeUnit.SECONDS);
            }
            if (count != null && count > rateLimit.count()) {
                throw new BizException(ResultCode.TOO_MANY_REQUESTS,
                        "操作过于频繁，请 " + rateLimit.seconds() + " 秒后再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("限流校验跳过（Redis 异常）: {}", e.getMessage());
        }
        return point.proceed();
    }

    private String resolveIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return "u" + ((LoginUser) authentication.getPrincipal()).getId();
        }
        return "ip" + WebUtils.getClientIp();
    }
}
