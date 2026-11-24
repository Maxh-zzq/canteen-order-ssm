package com.xatu.canteen.security;

import com.xatu.canteen.common.CanteenProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与校验。Access Token 无状态校验，Refresh Token 额外存 Redis 以支持撤销。
 */
@Component
public class JwtTokenProvider {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final CanteenProperties properties;
    private Key signingKey;

    public JwtTokenProvider(CanteenProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("canteen.jwt.secret 至少需要 32 个字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, String role, String jti) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", TYPE_ACCESS)
                .setId(jti)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getJwt().getAccessTokenSeconds() * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId, String jti) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("type", TYPE_REFRESH)
                .setId(jti)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getJwt().getRefreshTokenSeconds() * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }

    public String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
