package com.jieyou.photography.module.user.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.user.service.UserService;
import com.jieyou.photography.module.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户信息接口，依据 API 约定 5.1.4。
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.success(userService.getCurrentUser());
    }
}
