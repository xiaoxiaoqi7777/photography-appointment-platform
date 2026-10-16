package com.jieyou.photography.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求，依据 API 约定 5.1.2。
 */
public record LoginRequest(@NotBlank(message = "用户名不能为空") String username,
                           @NotBlank(message = "密码不能为空") String password) {
}
