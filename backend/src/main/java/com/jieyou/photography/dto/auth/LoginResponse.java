package com.jieyou.photography.dto.auth;

/**
 * 登录响应，依据 API 约定 5.1.2：token、role、expiresIn（秒）。
 */
public record LoginResponse(String token, String role, long expiresIn) {
}
