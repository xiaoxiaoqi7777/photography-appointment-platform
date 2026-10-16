package com.jieyou.photography.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求，依据 API 约定 5.1.1：用户名 3~20 全局唯一、密码 ≥6、昵称/手机号可选。
 * 角色由服务端固定为 USER，请求中的 role 等额外字段不参与解析。
 */
public record RegisterRequest(@NotBlank(message = "用户名不能为空")
                              @Size(min = 3, max = 20, message = "用户名长度须为 3~20 个字符")
                              String username,
                              @NotBlank(message = "密码不能为空")
                              @Size(min = 6, message = "密码长度至少 6 个字符")
                              String password,
                              @Size(max = 50, message = "昵称最长 50 个字符")
                              String nickname,
                              @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
                              String phone) {
}
