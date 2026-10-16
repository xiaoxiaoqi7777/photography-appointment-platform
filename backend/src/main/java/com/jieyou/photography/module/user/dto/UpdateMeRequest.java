package com.jieyou.photography.module.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 个人信息维护请求，依据 API 约定 5.1.5：字段可选，仅提交需要修改的项；
 * 修改密码时 oldPassword 与 newPassword 必须同时提供。
 */
public record UpdateMeRequest(@Size(max = 50, message = "昵称最长 50 个字符")
                              String nickname,
                              @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
                              String phone,
                              String oldPassword,
                              @Size(min = 6, message = "新密码长度至少 6 个字符")
                              String newPassword) {
}
