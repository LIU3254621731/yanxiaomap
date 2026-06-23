package com.yanxiaomap.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yanxiaomap.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 搜索用户列表
     */
    Page<User> searchUsers(Map<String, Object> params);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    User findByEmail(String email);

    /**
     * 根据手机号查询用户
     */
    User findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean checkUsernameExists(String username, Integer excludeId);

    /**
     * 检查邮箱是否存在
     */
    boolean checkEmailExists(String email, Integer excludeId);

    /**
     * 检查手机号是否存在
     */
    boolean checkPhoneExists(String phone, Integer excludeId);

    /**
     * 用户登录
     */
    Map<String, Object> login(String username, String password);

    /**
     * 用户注册
     */
    Map<String, Object> register(User user);

    /**
     * 更新用户最后登录信息
     */
    void updateLoginInfo(Integer userId, String ip);

    /**
     * 修改用户密码
     */
    boolean changePassword(Integer userId, String oldPassword, String newPassword);

    /**
     * 重置用户密码
     */
    boolean resetPassword(Integer userId, String newPassword);

    /**
     * 获取用户统计信息
     */
    Map<String, Long> getUserStatistics();
}