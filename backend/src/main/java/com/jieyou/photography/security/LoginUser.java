package com.jieyou.photography.security;

/**
 * 登录用户上下文对象（Security principal）。
 */
public record LoginUser(Long id, String username, String role) {
}
