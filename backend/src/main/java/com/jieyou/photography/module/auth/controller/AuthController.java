package com.jieyou.photography.module.auth.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.auth.dto.LoginRequest;
import com.jieyou.photography.module.auth.dto.LoginResponse;
import com.jieyou.photography.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口，依据 API 约定 5.1.1/5.1.2。
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
