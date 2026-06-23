package com.yanxiaomap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yanxiaomap.entity.User;
import com.yanxiaomap.mapper.UserMapper;
import com.yanxiaomap.service.UserService;
import com.yanxiaomap.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Page<User> searchUsers(Map<String, Object> params) {
        LambdaQueryWrapper<User> queryWrapper = buildSearchQueryWrapper(params);
        
        Integer page = (Integer) params.getOrDefault("page", 1);
        Integer size = (Integer) params.getOrDefault("size", 20);
        
        Page<User> pageObj = new Page<>(page, size);
        return baseMapper.selectPage(pageObj, queryWrapper);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getUsername, username.trim());
        queryWrapper.eq(User::getStatus, 1); // 只查询启用状态的用户
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, email.trim());
        queryWrapper.eq(User::getStatus, 1);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public User findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getPhone, phone.trim());
        queryWrapper.eq(User::getStatus, 1);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean checkUsernameExists(String username, Integer excludeId) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getUsername, username.trim());
        if (excludeId != null) {
            queryWrapper.ne(User::getId, excludeId);
        }
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean checkEmailExists(String email, Integer excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, email.trim());
        if (excludeId != null) {
            queryWrapper.ne(User::getId, excludeId);
        }
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean checkPhoneExists(String phone, Integer excludeId) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getPhone, phone.trim());
        if (excludeId != null) {
            queryWrapper.ne(User::getId, excludeId);
        }
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        log.info("用户登录尝试: username={}", username);
        
        // 查找用户
        User user = findByUsername(username);
        if (user == null) {
            log.warn("登录失败: 用户不存在 username={}", username);
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }
        
        // 检查用户状态
        if (user.getStatus() != 1) {
            log.warn("登录失败: 用户状态异常 username={}, status={}", username, user.getStatus());
            result.put("success", false);
            result.put("message", "用户账户已禁用");
            return result;
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败: 密码错误 username={}", username);
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }
        
        // 创建UserDetails对象用于生成JWT
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .disabled(user.getStatus() != 1)
                .build();
        
        // 生成JWT Token
        String token = jwtUtil.generateToken(userDetails);
        
        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        // 注意：实际IP应该从请求中获取，这里使用占位符
        user.setLastLoginIp("127.0.0.1");
        baseMapper.updateById(user);
        
        // 准备返回的用户信息（排除敏感信息）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("lastLoginTime", user.getLastLoginTime());
        
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("user", userInfo);
        result.put("token", token);
        result.put("expiresIn", jwtUtil.getExpirationDateFromToken(token));
        
        log.info("用户登录成功: username={}, userId={}", username, user.getId());
        return result;
    }

    @Override
    public Map<String, Object> register(User user) {
        Map<String, Object> result = new HashMap<>();
        
        log.info("用户注册尝试: username={}", user.getUsername());
        
        // 验证必填字段
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "密码不能为空");
            return result;
        }
        
        // 验证用户名是否已存在
        if (checkUsernameExists(user.getUsername(), null)) {
            log.warn("注册失败: 用户名已存在 username={}", user.getUsername());
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }
        
        // 验证邮箱是否已存在（如果邮箱不为空）
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (checkEmailExists(user.getEmail(), null)) {
                log.warn("注册失败: 邮箱已存在 email={}", user.getEmail());
                result.put("success", false);
                result.put("message", "邮箱已存在");
                return result;
            }
        }
        
        // 验证手机号是否已存在（如果手机号不为空）
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (checkPhoneExists(user.getPhone(), null)) {
                log.warn("注册失败: 手机号已存在 phone={}", user.getPhone());
                result.put("success", false);
                result.put("message", "手机号已存在");
                return result;
            }
        }
        
        // 设置默认值
        user.setStatus(1);
        user.setRole("user");
        user.setRegisterTime(LocalDateTime.now());
        user.setRegisterIp("127.0.0.1");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 保存用户
        try {
            baseMapper.insert(user);
            log.info("用户注册成功: username={}, userId={}", user.getUsername(), user.getId());
            
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("userId", user.getId());
        } catch (Exception e) {
            log.error("用户注册失败: username={}, error={}", user.getUsername(), e.getMessage(), e);
            result.put("success", false);
            result.put("message", "注册失败，请稍后重试");
        }
        
        return result;
    }

    @Override
    public void updateLoginInfo(Integer userId, String ip) {
        if (userId == null) {
            return;
        }
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        baseMapper.updateById(user);
    }

    @Override
    public boolean changePassword(Integer userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || newPassword == null) {
            return false;
        }
        
        log.info("用户修改密码: userId={}", userId);
        
        // 查询用户
        User user = baseMapper.selectById(userId);
        if (user == null) {
            log.warn("修改密码失败: 用户不存在 userId={}", userId);
            return false;
        }
        
        // 检查用户状态
        if (user.getStatus() != 1) {
            log.warn("修改密码失败: 用户状态异常 userId={}, status={}", userId, user.getStatus());
            return false;
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("修改密码失败: 旧密码错误 userId={}", userId);
            return false;
        }
        
        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("修改密码失败: 新密码不能与旧密码相同 userId={}", userId);
            return false;
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        int result = baseMapper.updateById(user);
        if (result > 0) {
            log.info("密码修改成功: userId={}", userId);
            return true;
        } else {
            log.error("密码修改失败: userId={}", userId);
            return false;
        }
    }

    @Override
    public boolean resetPassword(Integer userId, String newPassword) {
        if (userId == null || newPassword == null) {
            return false;
        }
        
        log.info("管理员重置用户密码: userId={}", userId);
        
        // 查询用户
        User user = baseMapper.selectById(userId);
        if (user == null) {
            log.warn("重置密码失败: 用户不存在 userId={}", userId);
            return false;
        }
        
        // 新密码不能与原密码相同（管理员重置时也建议检查）
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("重置密码失败: 新密码不能与原密码相同 userId={}", userId);
            return false;
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        int result = baseMapper.updateById(user);
        if (result > 0) {
            log.info("密码重置成功: userId={}", userId);
            return true;
        } else {
            log.error("密码重置失败: userId={}", userId);
            return false;
        }
    }

    @Override
    public Map<String, Long> getUserStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        // 查询总数
        long total = baseMapper.selectCount(Wrappers.lambdaQuery());
        statistics.put("total", total);
        
        // 查询启用状态的用户数
        long enabled = baseMapper.selectCount(
            Wrappers.<User>lambdaQuery().eq(User::getStatus, 1)
        );
        statistics.put("enabled", enabled);
        
        // 查询管理员用户数
        long adminCount = baseMapper.selectCount(
            Wrappers.<User>lambdaQuery().eq(User::getRole, "admin")
        );
        statistics.put("adminCount", adminCount);
        
        // 查询普通用户数
        long userCount = baseMapper.selectCount(
            Wrappers.<User>lambdaQuery().eq(User::getRole, "user")
        );
        statistics.put("userCount", userCount);
        
        // 查询今日新增用户数（简化：查询今天创建的用户）
        LambdaQueryWrapper<User> todayQuery = Wrappers.lambdaQuery();
        todayQuery.ge(User::getCreatedAt, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        long todayNew = baseMapper.selectCount(todayQuery);
        statistics.put("todayNew", todayNew);
        
        return statistics;
    }

    /**
     * 构建搜索查询条件
     */
    private LambdaQueryWrapper<User> buildSearchQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        
        // 关键词搜索（用户名、邮箱、手机号、真实姓名）
        if (params.containsKey("keyword") && params.get("keyword") != null) {
            String keyword = (String) params.get("keyword");
            queryWrapper.and(wrapper -> wrapper
                .like(User::getUsername, keyword)
                .or()
                .like(User::getEmail, keyword)
                .or()
                .like(User::getPhone, keyword)
                .or()
                .like(User::getRealName, keyword)
            );
        }
        
        // 用户状态筛选
        if (params.containsKey("status") && params.get("status") != null) {
            queryWrapper.eq(User::getStatus, params.get("status"));
        }
        
        // 用户角色筛选
        if (params.containsKey("role") && params.get("role") != null) {
            queryWrapper.eq(User::getRole, params.get("role"));
        }
        
        // 注册时间范围筛选
        if (params.containsKey("registerStart") && params.get("registerStart") != null) {
            try {
                // 假设传递的是字符串格式的时间，如 "2026-04-01" 或 "2026-04-01 00:00:00"
                String registerStartStr = params.get("registerStart").toString();
                LocalDateTime startTime;
                if (registerStartStr.length() == 10) {
                    // 日期格式 "yyyy-MM-dd"
                    startTime = LocalDateTime.parse(registerStartStr + "T00:00:00");
                } else {
                    // 日期时间格式 "yyyy-MM-dd HH:mm:ss"
                    String normalized = registerStartStr.replace(" ", "T");
                    startTime = LocalDateTime.parse(normalized);
                }
                queryWrapper.ge(User::getCreatedAt, startTime);
            } catch (Exception e) {
                log.warn("注册开始时间格式错误: {}, 将忽略此条件", params.get("registerStart"), e);
            }
        }
        if (params.containsKey("registerEnd") && params.get("registerEnd") != null) {
            try {
                // 假设传递的是字符串格式的时间，如 "2026-04-30" 或 "2026-04-30 23:59:59"
                String registerEndStr = params.get("registerEnd").toString();
                LocalDateTime endTime;
                if (registerEndStr.length() == 10) {
                    // 日期格式 "yyyy-MM-dd"
                    endTime = LocalDateTime.parse(registerEndStr + "T23:59:59");
                } else {
                    // 日期时间格式 "yyyy-MM-dd HH:mm:ss"
                    String normalized = registerEndStr.replace(" ", "T");
                    endTime = LocalDateTime.parse(normalized);
                }
                queryWrapper.le(User::getCreatedAt, endTime);
            } catch (Exception e) {
                log.warn("注册结束时间格式错误: {}, 将忽略此条件", params.get("registerEnd"), e);
            }
        }
        
        // 默认排序：按创建时间倒序
        queryWrapper.orderByDesc(User::getCreatedAt);
        
        return queryWrapper;
    }
}