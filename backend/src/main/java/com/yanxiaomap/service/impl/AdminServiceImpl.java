package com.yanxiaomap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yanxiaomap.entity.Admin;
import com.yanxiaomap.mapper.AdminMapper;
import com.yanxiaomap.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员服务实现类
 */
@Slf4j
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 最大登录失败次数
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    // 锁定时间（分钟）
    private static final int ACCOUNT_LOCK_MINUTES = 30;

    @Override
    public Admin findByUsername(String username) {
        LambdaQueryWrapper<Admin> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Admin::getUsername, username);
        queryWrapper.isNull(Admin::getDeletedAt); // 软删除过滤
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean validateAdmin(String username, String password) {
        Admin admin = findByUsername(username);
        
        if (admin == null) {
            log.warn("管理员账号不存在: {}", username);
            return false;
        }
        
        // 检查账户状态
        if (admin.getStatus() == 0) {
            log.warn("管理员账号已禁用: {}", username);
            return false;
        }
        
        // 检查账户是否被锁定
        if (isAdminLocked(admin.getId())) {
            log.warn("管理员账号已锁定: {}", username);
            return false;
        }
        
        // 验证密码
        boolean passwordValid = passwordEncoder.matches(password, admin.getPassword());
        
        if (passwordValid) {
            // 密码正确，重置登录失败次数，更新最后登录时间
            resetLoginFailCount(admin.getId());
            updateLastLoginTime(admin.getId());
            log.info("管理员登录成功: {}", username);
            return true;
        } else {
            // 密码错误，增加登录失败次数
            incrementLoginFailCount(admin.getId());
            
            // 检查是否需要锁定账户
            Admin updatedAdmin = baseMapper.selectById(admin.getId());
            if (updatedAdmin.getLoginFailCount() >= MAX_LOGIN_FAIL_COUNT) {
                lockAdminAccount(admin.getId(), ACCOUNT_LOCK_MINUTES);
                log.warn("管理员账号因多次登录失败被锁定: {}", username);
            }
            
            log.warn("管理员密码错误: {}", username);
            return false;
        }
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Integer adminId) {
        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setLastLoginAt(LocalDateTime.now());
        baseMapper.updateById(admin);
    }

    @Override
    @Transactional
    public void resetLoginFailCount(Integer adminId) {
        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        baseMapper.updateById(admin);
    }

    @Override
    @Transactional
    public void incrementLoginFailCount(Integer adminId) {
        Admin admin = baseMapper.selectById(adminId);
        if (admin != null) {
            Admin updateAdmin = new Admin();
            updateAdmin.setId(adminId);
            updateAdmin.setLoginFailCount(admin.getLoginFailCount() + 1);
            baseMapper.updateById(updateAdmin);
        }
    }

    @Override
    @Transactional
    public void lockAdminAccount(Integer adminId, int lockMinutes) {
        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        baseMapper.updateById(admin);
    }

    @Override
    @Transactional
    public void unlockAdminAccount(Integer adminId) {
        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setLockedUntil(null);
        admin.setLoginFailCount(0);
        baseMapper.updateById(admin);
    }

    @Override
    public boolean isAdminLocked(Integer adminId) {
        Admin admin = baseMapper.selectById(adminId);
        if (admin == null || admin.getLockedUntil() == null) {
            return false;
        }
        
        // 检查锁定是否已过期
        if (admin.getLockedUntil().isBefore(LocalDateTime.now())) {
            // 锁定已过期，自动解锁
            unlockAdminAccount(adminId);
            return false;
        }
        
        return true;
    }

    @Override
    public boolean checkEmailExists(String email, Integer excludeId) {
        LambdaQueryWrapper<Admin> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Admin::getEmail, email);
        queryWrapper.isNull(Admin::getDeletedAt);
        
        if (excludeId != null) {
            queryWrapper.ne(Admin::getId, excludeId);
        }
        
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 查询管理员总数
        long total = baseMapper.selectCount(Wrappers.lambdaQuery());
        statistics.put("total", total);
        
        // 查询启用状态的管理员数
        long enabled = baseMapper.selectCount(
            Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getStatus, 1)
                .isNull(Admin::getDeletedAt)
        );
        statistics.put("enabled", enabled);
        
        // 查询最近7天登录的管理员数
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentLogin = baseMapper.selectCount(
            Wrappers.<Admin>lambdaQuery()
                .ge(Admin::getLastLoginAt, sevenDaysAgo)
                .isNull(Admin::getDeletedAt)
        );
        statistics.put("recentLogin", recentLogin);
        
        // 查询被锁定的管理员数
        LocalDateTime now = LocalDateTime.now();
        long locked = baseMapper.selectCount(
            Wrappers.<Admin>lambdaQuery()
                .gt(Admin::getLockedUntil, now)
                .isNull(Admin::getDeletedAt)
        );
        statistics.put("locked", locked);
        
        return statistics;
    }

    @Override
    @Transactional
    public boolean changePassword(Integer adminId, String oldPassword, String newPassword) {
        Admin admin = baseMapper.selectById(adminId);
        if (admin == null) {
            log.error("管理员不存在: {}", adminId);
            return false;
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            log.warn("管理员密码修改失败: 旧密码错误, adminId={}", adminId);
            return false;
        }
        
        // 更新新密码
        Admin updateAdmin = new Admin();
        updateAdmin.setId(adminId);
        updateAdmin.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(updateAdmin);
        
        log.info("管理员密码修改成功: {}", adminId);
        return true;
    }

    @Override
    @Transactional
    public boolean resetPassword(Integer adminId, String newPassword) {
        Admin admin = baseMapper.selectById(adminId);
        if (admin == null) {
            log.error("管理员不存在: {}", adminId);
            return false;
        }
        
        Admin updateAdmin = new Admin();
        updateAdmin.setId(adminId);
        updateAdmin.setPassword(passwordEncoder.encode(newPassword));
        updateAdmin.setLoginFailCount(0);
        updateAdmin.setLockedUntil(null);
        baseMapper.updateById(updateAdmin);
        
        log.info("管理员密码重置成功: {}", adminId);
        return true;
    }

    @Override
    @Transactional
    public boolean updateAdminStatus(Integer adminId, Integer status) {
        Admin admin = baseMapper.selectById(adminId);
        if (admin == null) {
            log.error("管理员不存在: {}", adminId);
            return false;
        }
        
        Admin updateAdmin = new Admin();
        updateAdmin.setId(adminId);
        updateAdmin.setStatus(status);
        
        // 如果禁用账户，同时解锁账户
        if (status == 0) {
            updateAdmin.setLockedUntil(null);
            updateAdmin.setLoginFailCount(0);
        }
        
        baseMapper.updateById(updateAdmin);
        
        log.info("管理员状态更新: adminId={}, status={}", adminId, status);
        return true;
    }
}