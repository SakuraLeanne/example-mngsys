package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.mapper.PortalUserMapper;
import com.example.mngsys.portal.service.PortalUserService;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * PortalUserServiceImpl。
 * <p>
 * 用户服务实现，统一在持久化前做数据标准化与唯一性校验。
 * </p>
 */
@Service
public class PortalUserServiceImpl extends ServiceImpl<PortalUserMapper, PortalUser> implements PortalUserService {

    /**
     * 保存用户前进行标准化处理。
     *
     * @param entity 用户实体
     * @return 是否保存成功
     */
    @Override
    public boolean save(PortalUser entity) {
        normalizeUser(entity);
        return super.save(entity);
    }

    /**
     * 保存或更新用户前进行标准化处理。
     *
     * @param entity 用户实体
     * @return 是否保存成功
     */
    @Override
    public boolean saveOrUpdate(PortalUser entity) {
        normalizeUser(entity);
        return super.saveOrUpdate(entity);
    }

    /**
     * 根据 ID 更新用户前进行标准化处理。
     *
     * @param entity 用户实体
     * @return 是否更新成功
     */
    @Override
    public boolean updateById(PortalUser entity) {
        normalizeUser(entity);
        return super.updateById(entity);
    }

    /**
     * 统一对用户信息做默认值与唯一性校验。
     *
     * @param user 用户实体
     */
    private void normalizeUser(PortalUser user) {
        if (user == null) {
            return;
        }
        if (!StringUtils.hasText(user.getMobile())) {
            throw new IllegalArgumentException("mobile 不能为空");
        }
        if (!StringUtils.hasText(user.getUsername())) {
            user.setUsername(user.getMobile());
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("password 不能为空");
        }
        if (user.getMobileVerified() == null) {
            user.setMobileVerified(0);
        }
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(0);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        ensureUnique(user);
    }

    /**
     * 校验手机号、用户名唯一性，避免数据库异常。
     *
     * @param user 用户实体
     */
    private void ensureUnique(PortalUser user) {
        LambdaQueryWrapper<PortalUser> mobileWrapper = new LambdaQueryWrapper<>();
        mobileWrapper.eq(PortalUser::getMobile, user.getMobile());
        if (StringUtils.hasText(user.getId())) {
            mobileWrapper.ne(PortalUser::getId, user.getId());
        }
        if (count(mobileWrapper) > 0) {
            throw new IllegalArgumentException("手机号已存在");
        }

        LambdaQueryWrapper<PortalUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(PortalUser::getUsername, user.getUsername());
        if (StringUtils.hasText(user.getId())) {
            usernameWrapper.ne(PortalUser::getId, user.getId());
        }
        if (count(usernameWrapper) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }
}
