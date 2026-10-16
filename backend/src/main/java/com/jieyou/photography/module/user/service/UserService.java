package com.jieyou.photography.module.user.service;

import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.user.dto.UpdateMeRequest;
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.module.user.vo.UserVO;
import com.jieyou.photography.security.LoginUser;
import com.jieyou.photography.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务逻辑。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取当前登录用户信息（GET /api/users/me）。
     */
    public UserVO getCurrentUser() {
        return toVO(loadCurrentUser());
    }

    /**
     * 维护个人信息（PUT /api/users/me），依据 API 约定 5.1.5：
     * nickname/phone 可选单独更新；修改密码需同时提供 oldPassword 与 newPassword。
     */
    public UserVO updateCurrentUser(UpdateMeRequest request) {
        User user = loadCurrentUser();

        String nickname = trimToNull(request.nickname());
        String phone = trimToNull(request.phone());
        String oldPassword = trimToNull(request.oldPassword());
        String newPassword = trimToNull(request.newPassword());

        if (request.nickname() != null && nickname == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "昵称不能为空");
        }
        if (request.phone() != null && phone == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "手机号不能为空");
        }
        boolean changePassword = oldPassword != null || newPassword != null;
        if (changePassword && (oldPassword == null || newPassword == null)) {
            throw new BizException(ResultCode.PARAM_INVALID, "修改密码需同时提供原密码与新密码");
        }
        if (nickname == null && phone == null && !changePassword) {
            throw new BizException(ResultCode.PARAM_INVALID, "没有需要更新的字段");
        }
        if (changePassword) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BizException(ResultCode.PARAM_INVALID, "原密码错误");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    private User loadCurrentUser() {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userMapper.selectById(loginUser.id());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        return user;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(),
                user.getAvatar(), user.getRole(), user.getStatus(), user.getCreateTime());
    }
}
