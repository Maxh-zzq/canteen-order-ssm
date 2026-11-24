package com.xatu.canteen.security;

import com.xatu.canteen.common.CanteenProperties;
import com.xatu.canteen.common.RedisKeys;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。校验签名、类型、黑名单、单端登录会话。
 * Redis 故障时仅跳过黑名单校验，保证可用性（生产应告警）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redis;
    private final CanteenProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = tokenProvider.parse(token);
                if (!JwtTokenProvider.TYPE_ACCESS.equals(claims.get("type", String.class))) {
                    chain.doFilter(request, response);
                    return;
                }
                if (!isTokenActive(claims)) {
                    chain.doFilter(request, response);
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                Long expireAt = claims.getExpiration() == null ? null : claims.getExpiration().getTime() / 1000;
                LoginUser loginUser = new LoginUser(userId, username, role, claims.getId(), expireAt);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        loginUser, null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.debug("Token 校验失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isTokenActive(Claims claims) {
        String jti = claims.getId();
        Long userId = Long.valueOf(claims.getSubject());
        try {
            if (Boolean.TRUE.equals(redis.hasKey(RedisKeys.blacklist(jti)))) {
                return false;
            }
            if (properties.getJwt().isSingleLogin()) {
                String activeJti = redis.opsForValue().get(RedisKeys.activeSession(userId));
                if (StringUtils.hasText(activeJti) && !activeJti.equals(jti)) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Redis 不可用，跳过 Token 会话校验: {}", e.getMessage());
        }
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            return header.substring(BEARER.length()).trim();
        }
        return null;
    }
}
