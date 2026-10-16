package com.jieyou.photography.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.auth.dto.LoginRequest;
import com.jieyou.photography.module.auth.dto.LoginResponse;
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证业务逻辑，依据 API 约定 5.1.2：
 * 按用户名查询、BCrypt 校验、status=1 校验、签发 24 小时 JWT。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getRole(), jwtService.getExpirationSeconds());
    }
}
