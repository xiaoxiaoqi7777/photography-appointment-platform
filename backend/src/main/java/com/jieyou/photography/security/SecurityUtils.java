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
        LoginUser user = currentUserOrNull();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }

    /**
     * 当前登录用户，匿名访问（公开接口未携带令牌）时返回 null。
     */
    public static LoginUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            return null;
        }
        return user;
    }

    public static boolean isAdmin() {
        LoginUser user = currentUserOrNull();
        return user != null && "ADMIN".equals(user.role());
    }
}
