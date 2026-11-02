package com.jieyou.photography.controller.auth;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.dto.auth.LoginRequest;
import com.jieyou.photography.dto.auth.LoginResponse;
import com.jieyou.photography.dto.auth.RegisterRequest;
import com.jieyou.photography.service.auth.AuthService;
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

    @PostMapping("/api/auth/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }
}
