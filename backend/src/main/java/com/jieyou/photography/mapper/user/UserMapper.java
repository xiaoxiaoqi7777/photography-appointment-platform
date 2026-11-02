package com.jieyou.photography.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jieyou.photography.entity.user.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
