package com.jieyou.photography.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.auth.dto.LoginRequest;
import com.jieyou.photography.module.auth.dto.LoginResponse;
import com.jieyou.photography.module.auth.dto.RegisterRequest;
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    /**
     * 用户注册，依据 API 约定 5.1.1：用户名唯一、密码 BCrypt 存储、
     * 角色服务端固定为 USER、status=1。
     */
    public void register(RegisterRequest request) {
        String username = trimToNull(request.username());
        if (username == null || username.length() < 3 || username.length() > 20) {
            throw new BizException(ResultCode.PARAM_INVALID, "用户名长度须为 3~20 个字符");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.DUPLICATE, "用户名已被占用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(trimToNull(request.nickname()));
        user.setPhone(trimToNull(request.phone()));
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // uk_username 并发冲突：按唯一性冲突口径返回 40901
            throw new BizException(ResultCode.DUPLICATE, "用户名已被占用");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
