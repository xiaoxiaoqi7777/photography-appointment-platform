package com.jieyou.photography.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtService 单元测试（不依赖数据库与 Spring 容器）。
 */
class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 24);
    }

    @Test
    void generateAndParseToken() {
        String token = jwtService.generateToken(1L, "admin", "ADMIN");
        Claims claims = jwtService.parse(token);
        assertEquals("admin", claims.getSubject());
        assertEquals(1L, ((Number) claims.get("uid")).longValue());
        assertEquals("ADMIN", claims.get("role"));
        assertEquals(86400L, jwtService.getExpirationSeconds());
    }

    @Test
    void parseInvalidTokenThrows() {
        assertThrows(JwtException.class, () -> jwtService.parse("not-a-valid-token"));
    }

    @Test
    void parseExpiredTokenThrows() {
        JwtService expiredService = new JwtService(SECRET, 0);
        String token = expiredService.generateToken(1L, "admin", "ADMIN");
        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void shortSecretRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 24));
    }
}
