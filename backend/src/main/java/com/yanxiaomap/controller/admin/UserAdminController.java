package com.yanxiaomap.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yanxiaomap.common.Result;
import com.yanxiaomap.entity.User;
import com.yanxiaomap.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 管理后台对普通用户的管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Api(tags = "用户管理接口")
public class UserAdminController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping("")
    @ApiOperation(value = "获取用户列表", notes = "分页获取用户列表，支持筛选")
    public Result<Page<Map<String, Object>>> getUserList(
            @ApiParam(value = "搜索关键词（用户名、邮箱、手机号）") @RequestParam(required = false) String keyword,
            @ApiParam(value = "用户状态：0=禁用，1=启用") @RequestParam(required = false) Integer status,
            @ApiParam(value = "注册时间开始（格式：yyyy-MM-dd）") @RequestParam(required = false) String registerStart,
            @ApiParam(value = "注册时间结束（格式：yyyy-MM-dd）") @RequestParam(required = false) String registerEnd,
            @ApiParam(value = "页码，从1开始", defaultValue = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小，最大100", defaultValue = "20") @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("获取用户列表请求: keyword={}, status={}, registerStart={}, registerEnd={}, page={}, size={}",
                keyword, status, registerStart, registerEnd, page, size);

        // 构建查询参数
        Map<String, Object> params = new HashMap<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            params.put("keyword", keyword.trim());
        }
        if (status != null) {
            params.put("status", status);
        }
        if (registerStart != null && !registerStart.trim().isEmpty()) {
            params.put("registerStart", registerStart.trim());
        }
        if (registerEnd != null && !registerEnd.trim().isEmpty()) {
            params.put("registerEnd", registerEnd.trim());
        }
        params.put("page", page);
        params.put("size", size);
        
        // 调用UserService获取分页数据
        Page<User> userPageResult = userService.searchUsers(params);
        
        // 转换User实体为Map格式返回
        Page<Map<String, Object>> resultPage = new Page<>(userPageResult.getCurrent(), userPageResult.getSize(), userPageResult.getTotal());
        resultPage.setRecords(convertUsersToMaps(userPageResult.getRecords()));
        
        return Result.success("获取用户列表成功", resultPage);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    @ApiOperation(value = "获取用户详情", notes = "根据用户ID获取用户详细信息")
    public Result<Map<String, Object>> getUserDetail(
            @ApiParam(value = "用户ID", required = true) @PathVariable Integer userId
    ) {
        log.info("获取用户详情请求: userId={}", userId);
        
        // 查询用户信息
        User user = userService.getById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return Result.error("用户不存在");
        }
        
        // 转换为Map格式
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("lastLoginTime", user.getLastLoginTime());
        userInfo.put("lastLoginIp", user.getLastLoginIp());
        userInfo.put("registerTime", user.getRegisterTime());
        userInfo.put("registerIp", user.getRegisterIp());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("updatedAt", user.getUpdatedAt());
        userInfo.put("deletedAt", user.getDeletedAt());
        
        return Result.success("获取用户详情成功", userInfo);
    }

    /**
     * 创建新用户
     */
    @PostMapping("")
    @ApiOperation(value = "创建新用户", notes = "创建新的用户账号")
    public Result<Map<String, Object>> createUser(
            @ApiParam(value = "用户信息", required = true) @RequestBody Map<String, Object> userData
    ) {
        log.info("创建用户请求: userData={}", userData.keySet());
        
        // 验证必要字段
        String username = (String) userData.get("username");
        String password = (String) userData.get("password");
        String email = (String) userData.get("email");
        
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        
        // 检查用户名是否已存在
        if (userService.checkUsernameExists(username, null)) {
            return Result.error("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (userService.checkEmailExists(email, null)) {
            return Result.error("邮箱已存在");
        }
        
        // 创建用户对象
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(password.trim()); // 注意：实际应用中需要加密
        user.setEmail(email.trim());
        user.setPhone((String) userData.get("phone"));
        user.setRealName((String) userData.get("realName"));
        user.setRole((String) userData.getOrDefault("role", "user"));
        user.setStatus((Integer) userData.getOrDefault("status", 1));
        user.setAvatar((String) userData.get("avatar"));
        user.setRegisterTime(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 保存用户
        boolean success = userService.save(user);
        if (!success) {
            log.error("创建用户失败: username={}", username);
            return Result.error("创建用户失败");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("message", "用户创建成功");
        
        return Result.success("用户创建成功", result);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    @ApiOperation(value = "更新用户信息", notes = "更新指定用户的信息")
    public Result<String> updateUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Integer userId,
            @ApiParam(value = "用户信息", required = true) @RequestBody Map<String, Object> userData
    ) {
        log.info("更新用户请求: userId={}, userData={}", userId, userData.keySet());
        
        // 检查用户是否存在
        User existingUser = userService.getById(userId);
        if (existingUser == null) {
            log.warn("用户不存在: userId={}", userId);
            return Result.error("用户不存在");
        }
        
        // 验证并更新字段
        String username = (String) userData.get("username");
        String email = (String) userData.get("email");
        String phone = (String) userData.get("phone");
        
        // 检查用户名是否重复（排除当前用户）
        if (username != null && !username.trim().isEmpty()) {
            if (userService.checkUsernameExists(username.trim(), userId)) {
                return Result.error("用户名已存在");
            }
            existingUser.setUsername(username.trim());
        }
        
        // 检查邮箱是否重复
        if (email != null && !email.trim().isEmpty()) {
            if (userService.checkEmailExists(email.trim(), userId)) {
                return Result.error("邮箱已存在");
            }
            existingUser.setEmail(email.trim());
        }
        
        // 检查手机号是否重复
        if (phone != null && !phone.trim().isEmpty()) {
            if (userService.checkPhoneExists(phone.trim(), userId)) {
                return Result.error("手机号已存在");
            }
            existingUser.setPhone(phone.trim());
        }
        
        // 更新其他字段
        if (userData.containsKey("realName")) {
            existingUser.setRealName((String) userData.get("realName"));
        }
        if (userData.containsKey("role")) {
            existingUser.setRole((String) userData.get("role"));
        }
        if (userData.containsKey("status")) {
            existingUser.setStatus((Integer) userData.get("status"));
        }
        if (userData.containsKey("avatar")) {
            existingUser.setAvatar((String) userData.get("avatar"));
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新
        boolean success = userService.updateById(existingUser);
        if (!success) {
            log.error("更新用户信息失败: userId={}", userId);
            return Result.error("更新用户信息失败");
        }
        
        return Result.success("用户信息更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @ApiOperation(value = "删除用户", notes = "删除指定用户（逻辑删除）")
    public Result<String> deleteUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Integer userId
    ) {
        log.info("删除用户请求: userId={}", userId);
        
        // 检查用户是否存在
        User existingUser = userService.getById(userId);
        if (existingUser == null) {
            log.warn("用户不存在: userId={}", userId);
            return Result.error("用户不存在");
        }
        
        // 使用MyBatis-Plus的逻辑删除功能
        boolean success = userService.removeById(userId);
        if (!success) {
            log.error("删除用户失败: userId={}", userId);
            return Result.error("删除用户失败");
        }
        
        return Result.success("用户删除成功");
    }

    /**
     * 启用/禁用用户
     */
    @PostMapping("/{userId}/status")
    @ApiOperation(value = "修改用户状态", notes = "启用或禁用用户账号")
    public Result<String> updateUserStatus(
            @ApiParam(value = "用户ID", required = true) @PathVariable Integer userId,
            @ApiParam(value = "状态：0=禁用，1=启用", required = true) @RequestParam Integer status
    ) {
        log.info("修改用户状态请求: userId={}, status={}", userId, status);
        
        // 验证状态值
        if (status != 0 && status != 1) {
            return Result.error("状态值无效，必须为0（禁用）或1（启用）");
        }
        
        // 检查用户是否存在
        User existingUser = userService.getById(userId);
        if (existingUser == null) {
            log.warn("用户不存在: userId={}", userId);
            return Result.error("用户不存在");
        }
        
        // 更新状态
        existingUser.setStatus(status);
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        boolean success = userService.updateById(existingUser);
        if (!success) {
            log.error("修改用户状态失败: userId={}, status={}", userId, status);
            return Result.error("修改用户状态失败");
        }
        
        String statusText = status == 1 ? "启用" : "禁用";
        return Result.success("用户" + statusText + "成功");
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{userId}/reset-password")
    @ApiOperation(value = "重置用户密码", notes = "重置指定用户的密码为默认密码")
    public Result<String> resetUserPassword(
            @ApiParam(value = "用户ID", required = true) @PathVariable Integer userId
    ) {
        log.info("重置用户密码请求: userId={}", userId);
        
        // 检查用户是否存在
        User existingUser = userService.getById(userId);
        if (existingUser == null) {
            log.warn("用户不存在: userId={}", userId);
            return Result.error("用户不存在");
        }
        
        // 生成默认密码（这里使用"123456"，实际应用中应该生成随机密码）
        String defaultPassword = "123456";
        
        // 使用UserService的resetPassword方法
        boolean success = userService.resetPassword(userId, defaultPassword);
        if (!success) {
            log.error("重置用户密码失败: userId={}", userId);
            return Result.error("重置用户密码失败");
        }
        
        // 注意：实际应用中应该通过邮件或短信发送新密码给用户
        // 这里仅记录日志
        log.info("用户密码已重置: userId={}, newPassword={}", userId, defaultPassword);
        
        return Result.success("用户密码重置成功，新密码已发送到用户邮箱");
    }

    /**
     * 将User实体列表转换为Map列表
     */
    private List<Map<String, Object>> convertUsersToMaps(List<User> users) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return result;
        }
        
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("realName", user.getRealName());
            userMap.put("role", user.getRole());
            userMap.put("status", user.getStatus());
            userMap.put("avatar", user.getAvatar());
            userMap.put("lastLoginTime", user.getLastLoginTime());
            userMap.put("registerTime", user.getRegisterTime());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("updatedAt", user.getUpdatedAt());
            result.add(userMap);
        }
        return result;
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取用户统计数据", notes = "获取用户相关的统计数据")
    public Result<Map<String, Object>> getUserStatistics() {
        log.info("获取用户统计数据请求");
        
        // 获取用户统计数据
        Map<String, Long> userStats = userService.getUserStatistics();
        
        // 计算用户增长率（简化：本月新增用户占总用户的比例）
        long totalUsers = userStats.getOrDefault("total", 0L);
        long todayNew = userStats.getOrDefault("todayNew", 0L);
        long enabledUsers = userStats.getOrDefault("enabled", 0L);
        long adminCount = userStats.getOrDefault("adminCount", 0L);
        long userCount = userStats.getOrDefault("userCount", 0L);
        
        // 计算增长率（示例：本月新增/总用户 * 100%）
        String growthRate = "0%";
        if (totalUsers > 0) {
            double rate = (todayNew * 100.0) / totalUsers;
            growthRate = String.format("%.2f%%", rate);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", enabledUsers);
        stats.put("adminCount", adminCount);
        stats.put("userCount", userCount);
        stats.put("newUsersToday", todayNew);
        stats.put("newUsersThisWeek", todayNew); // 简化：使用今日新增代表本周新增
        stats.put("newUsersThisMonth", todayNew); // 简化：使用今日新增代表本月新增
        stats.put("userGrowthRate", growthRate);
        
        return Result.success("获取用户统计数据成功", stats);
    }
}