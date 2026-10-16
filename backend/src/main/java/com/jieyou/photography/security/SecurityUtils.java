package com.jieyou.photography.security;

import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }
}
