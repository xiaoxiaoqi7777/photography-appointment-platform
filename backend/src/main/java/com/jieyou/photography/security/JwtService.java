package com.jieyou.photography.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 签发与解析，依据 docs/API接口约定.md 2.2 节：
 * 有效期 24 小时，令牌中包含角色声明。
 */
@Component
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expireMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours:24}") long expireHours) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 未配置或长度不足 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generateToken(Long uid, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_UID, uid)
                .claim(CLAIM_ROLE, role)
                .issuedAt(new java.util.Date(now))
                .expiration(new java.util.Date(now + expireMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expireMillis / 1000;
    }
}
