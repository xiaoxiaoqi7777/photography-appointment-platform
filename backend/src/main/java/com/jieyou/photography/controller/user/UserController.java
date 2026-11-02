package com.jieyou.photography.controller.user;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.dto.user.UpdateMeRequest;
import com.jieyou.photography.service.user.UserService;
import com.jieyou.photography.vo.user.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户信息接口，依据 API 约定 5.1.4（查询）与 5.1.5（维护）。
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

    @PutMapping("/api/users/me")
    public ApiResponse<UserVO> updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return ApiResponse.success(userService.updateCurrentUser(request));
    }
}
