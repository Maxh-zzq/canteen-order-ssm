package com.xatu.canteen.common;

import com.xatu.canteen.entity.SysOperLog;
import com.xatu.canteen.mapper.SysOperLogMapper;
import com.xatu.canteen.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 操作日志切面：记录模块、方法、参数、耗时、IP、结果。日志写失败不影响业务。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String error = null;
        try {
            return point.proceed();
        } catch (Throwable t) {
            success = false;
            error = t.getMessage();
            throw t;
        } finally {
            save(point, operLog, start, success, error);
        }
    }

    private void save(ProceedingJoinPoint point, OperLog operLog, long start, boolean success, String error) {
        try {
            SysOperLog logEntity = new SysOperLog();
            logEntity.setTraceId(org.slf4j.MDC.get("traceId"));
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
                LoginUser user = (LoginUser) authentication.getPrincipal();
                logEntity.setUserId(user.getId());
                logEntity.setUsername(user.getUsername());
            } else {
                logEntity.setUsername("anonymous");
            }
            logEntity.setModule(operLog.module());
            logEntity.setMethod(point.getSignature().getDeclaringType().getSimpleName() + "." + point.getSignature().getName());
            logEntity.setParams(buildParams(point.getArgs()));
            logEntity.setIp(WebUtils.getClientIp());
            logEntity.setCostMs(System.currentTimeMillis() - start);
            logEntity.setSuccess(success ? 1 : 0);
            logEntity.setErrorMsg(StringUtils.hasText(error) && error.length() > 500 ? error.substring(0, 500) : error);
            logEntity.setCreateTime(LocalDateTime.now());
            operLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> printable = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
                continue;
            }
            printable.add(arg);
        }
        if (printable.isEmpty()) {
            return null;
        }
        try {
            String json = JsonUtil.toJson(printable.size() == 1 ? printable.get(0) : printable);
            return json.length() > 2000 ? json.substring(0, 2000) : json;
        } catch (Exception e) {
            return Arrays.toString(printable.toArray());
        }
    }
}
