package com.jieyou.photography.module.user.service;

import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import com.jieyou.photography.module.user.vo.UserVO;
import com.jieyou.photography.security.LoginUser;
import com.jieyou.photography.security.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * 用户业务逻辑。
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 获取当前登录用户信息（GET /api/users/me）。
     */
    public UserVO getCurrentUser() {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userMapper.selectById(loginUser.id());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(),
                user.getAvatar(), user.getRole(), user.getStatus(), user.getCreateTime());
    }
}
