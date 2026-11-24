package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.CanteenProperties;
import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.LoginDTO;
import com.xatu.canteen.entity.SysUser;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.security.JwtTokenProvider;
import com.xatu.canteen.security.LoginUser;
import com.xatu.canteen.vo.LoginVO;
import com.xatu.canteen.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * 认证服务：登录、刷新、登出。双 Token + Redis 会话，支持撤销与单端登录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redis;
    private final CanteenProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;

    public LoginVO login(LoginDTO dto) {
        if (!captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BizException(ResultCode.CAPTCHA_ERROR);
        }
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        // 用户不存在与密码错误返回同一提示，避免账号枚举
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        return issueTokens(user);
    }

    public LoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (!JwtTokenProvider.TYPE_REFRESH.equals(claims.get("type", String.class))) {
            throw new BizException(ResultCode.UNAUTHORIZED, "无效的刷新凭证");
        }
        Long userId = Long.valueOf(claims.getSubject());
        String stored;
        try {
            stored = redis.opsForValue().get(RedisKeys.refreshToken(userId));
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "会话服务暂不可用，请重新登录");
        }
        if (!StringUtils.hasText(stored) || !stored.equals(refreshToken)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        return issueTokens(user);
    }

    public void logout(LoginUser loginUser) {
        if (loginUser == null || !StringUtils.hasText(loginUser.getJti())) {
            return;
        }
        long ttl = properties.getJwt().getAccessTokenSeconds();
        if (loginUser.getExpireAt() != null) {
            ttl = loginUser.getExpireAt() - Instant.now().getEpochSecond();
        }
        ttl = Math.max(ttl, 1);
        try {
            redis.opsForValue().set(RedisKeys.blacklist(loginUser.getJti()), "1", Duration.ofSeconds(ttl));
            redis.delete(RedisKeys.refreshToken(loginUser.getId()));
            redis.delete(RedisKeys.activeSession(loginUser.getId()));
        } catch (Exception e) {
            log.error("登出时清理 Redis 会话失败: {}", e.getMessage());
            throw new BizException(ResultCode.SERVER_ERROR, "会话服务暂不可用，请稍后重试");
        }
    }

    private LoginVO issueTokens(SysUser user) {
        String role = StringUtils.hasText(user.getRoleCode()) ? user.getRoleCode() : "STUDENT";
        String accessJti = tokenProvider.newJti();
        String refreshJti = tokenProvider.newJti();
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getUsername(), role, accessJti);
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), refreshJti);
        try {
            redis.opsForValue().set(RedisKeys.refreshToken(user.getId()), refreshToken,
                    Duration.ofSeconds(properties.getJwt().getRefreshTokenSeconds()));
            if (properties.getJwt().isSingleLogin()) {
                redis.opsForValue().set(RedisKeys.activeSession(user.getId()), accessJti,
                        Duration.ofSeconds(properties.getJwt().getAccessTokenSeconds()));
            }
        } catch (Exception e) {
            // Redis 故障不影响登录，只影响撤销能力
            log.error("写入 Redis 会话失败: {}", e.getMessage());
        }
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(properties.getJwt().getAccessTokenSeconds());
        vo.setUser(UserVO.from(user));
        return vo;
    }
}
