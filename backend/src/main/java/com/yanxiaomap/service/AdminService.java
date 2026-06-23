package com.yanxiaomap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yanxiaomap.entity.Admin;
import java.util.Map;

/**
 * 管理员服务接口
 */
public interface AdminService extends IService<Admin> {

    /**
     * 根据用户名查找管理员
     */
    Admin findByUsername(String username);

    /**
     * 管理员登录验证
     */
    boolean validateAdmin(String username, String password);

    /**
     * 更新管理员最后登录时间
     */
    void updateLastLoginTime(Integer adminId);

    /**
     * 重置登录失败次数
     */
    void resetLoginFailCount(Integer adminId);

    /**
     * 增加登录失败次数
     */
    void incrementLoginFailCount(Integer adminId);

    /**
     * 锁定管理员账户
     */
    void lockAdminAccount(Integer adminId, int lockMinutes);

    /**
     * 解锁管理员账户
     */
    void unlockAdminAccount(Integer adminId);

    /**
     * 检查管理员账户是否被锁定
     */
    boolean isAdminLocked(Integer adminId);

    /**
     * 检查邮箱是否已存在
     */
    boolean checkEmailExists(String email, Integer excludeId);

    /**
     * 获取管理员统计信息
     */
    Map<String, Object> getAdminStatistics();

    /**
     * 修改管理员密码
     */
    boolean changePassword(Integer adminId, String oldPassword, String newPassword);

    /**
     * 重置管理员密码
     */
    boolean resetPassword(Integer adminId, String newPassword);

    /**
     * 启用/禁用管理员账户
     */
    boolean updateAdminStatus(Integer adminId, Integer status);
}