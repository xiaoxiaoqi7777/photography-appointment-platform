package com.jieyou.photography.module.user.vo;

import java.time.LocalDateTime;

/**
 * 当前用户信息响应，字段依据 API 约定 5.1.4（排除 password）。
 */
public record UserVO(Long id,
                     String username,
                     String nickname,
                     String phone,
                     String avatar,
                     String role,
                     Integer status,
                     LocalDateTime createTime) {
}
