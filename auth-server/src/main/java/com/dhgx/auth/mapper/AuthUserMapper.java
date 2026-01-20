package com.dhgx.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dhgx.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * AuthUserMapper。
 * <p>
 * 基于 MyBatis-Plus 的用户表 Mapper，提供基础 CRUD 能力。
 * </p>
 */
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}
