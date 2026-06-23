package com.yanxiaomap.controller.admin;

import com.yanxiaomap.common.Result;
import com.yanxiaomap.common.ResultCode;
import com.yanxiaomap.entity.Admin;
import com.yanxiaomap.service.AdminService;
import com.yanxiaomap.service.SchoolService;
import com.yanxiaomap.service.MajorService;
import com.yanxiaomap.service.UserService;
import com.yanxiaomap.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台控制器
 * 提供用户管理、数据管理、系统配置等功能
 * 需要管理员权限访问
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Api(tags = "管理后台接口")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private MajorService majorService;

    @Autowired
    private UserService userService;

    /**
     * 管理员登录接口
     */
    @PostMapping("/login")
    @ApiOperation(value = "管理员登录", notes = "管理员用户登录系统")
    public Result<Map<String, String>> adminLogin(
            @ApiParam(value = "用户名", required = true) @RequestParam String username,
            @ApiParam(value = "密码", required = true) @RequestParam String password
    ) {
        log.info("管理员登录请求: username={}", username);
        
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "密码不能为空");
        }
        
        try {
            // 验证管理员账号密码
            boolean isValid = adminService.validateAdmin(username, password);
            
            if (!isValid) {
                log.warn("管理员登录失败: username={}", username);
                return Result.error(ResultCode.UNAUTHORIZED, "用户名或密码错误");
            }
            
            // 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(userDetails);
            
            // 获取管理员详细信息
            Admin admin = adminService.findByUsername(username);
            
            // 构建响应结果
            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            result.put("tokenType", "Bearer");
            result.put("username", username);
            result.put("role", "admin");
            result.put("adminId", admin.getId().toString());
            result.put("email", admin.getEmail() != null ? admin.getEmail() : "");
            result.put("name", admin.getName() != null ? admin.getName() : "");
            result.put("avatar", admin.getAvatar() != null ? admin.getAvatar() : "");
            
            log.info("管理员登录成功: username={}, adminId={}", username, admin.getId());
            return Result.success("登录成功", result);
            
        } catch (Exception e) {
            log.error("管理员登录异常: username={}, error={}", username, e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        }
    }

    /**
     * 获取管理员信息
     */
    @GetMapping("/info")
    @ApiOperation(value = "获取管理员信息", notes = "获取当前登录管理员的信息")
    public Result<Map<String, Object>> getAdminInfo() {
        log.info("获取管理员信息请求");
        
        try {
            // 从SecurityContext获取当前认证信息
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Result.error(ResultCode.UNAUTHORIZED, "未登录或认证已过期");
            }
            
            String username = authentication.getName();
            Admin admin = adminService.findByUsername(username);
            
            if (admin == null) {
                return Result.error(ResultCode.NOT_FOUND, "管理员不存在");
            }
            
            // 构建管理员信息
            Map<String, Object> info = new HashMap<>();
            info.put("adminId", admin.getId());
            info.put("username", admin.getUsername());
            info.put("email", admin.getEmail() != null ? admin.getEmail() : "");
            info.put("name", admin.getName() != null ? admin.getName() : "");
            info.put("phone", admin.getPhone() != null ? admin.getPhone() : "");
            info.put("avatar", admin.getAvatar() != null ? admin.getAvatar() : "");
            info.put("status", admin.getStatus());
            info.put("role", "admin");
            
            // 构建权限列表
            info.put("permissions", new String[]{"user:manage", "data:manage", "system:config"});
            
            // 最后登录时间
            if (admin.getLastLoginAt() != null) {
                info.put("lastLoginTime", admin.getLastLoginAt());
            }
            
            log.info("获取管理员信息成功: username={}", username);
            return Result.success("获取管理员信息成功", info);
            
        } catch (Exception e) {
            log.error("获取管理员信息异常: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取管理员信息失败");
        }
    }

    /**
     * 获取系统统计数据
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取系统统计数据", notes = "获取平台整体统计数据")
    public Result<Map<String, Object>> getSystemStatistics() {
        log.info("获取系统统计数据请求");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 查询学校总数
            long totalSchools = schoolService.count();
            stats.put("totalSchools", totalSchools);
            
            // 查询专业总数
            long totalMajors = majorService.count();
            stats.put("totalMajors", totalMajors);
            
            // 查询招生数据总数（需要注入AdmissionDataService）
            // long totalAdmissionData = admissionDataService.count();
            // stats.put("totalAdmissionData", totalAdmissionData);
            stats.put("totalAdmissionData", 0); // 暂时设为0，需要时再实现
            
            // 查询用户总数
            long totalUsers = userService.count();
            stats.put("totalUsers", totalUsers);
            
            // 获取管理员统计数据
            Map<String, Object> adminStats = adminService.getAdminStatistics();
            stats.put("totalAdmins", adminStats.get("total"));
            stats.put("enabledAdmins", adminStats.get("enabled"));
            stats.put("lockedAdmins", adminStats.get("locked"));
            
            // 今日数据（暂用模拟数据）
            stats.put("todayVisits", 0);
            stats.put("todayRegistrations", 0);
            
            // 数据更新时间
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            stats.put("dataUpdateTime", currentTime);
            
            // 系统版本信息
            stats.put("systemVersion", "1.0.0");
            stats.put("lastDataSync", "2026-04-17 23:00:00");
            
            log.info("获取系统统计数据成功: totalSchools={}, totalMajors={}, totalUsers={}", 
                    totalSchools, totalMajors, totalUsers);
            return Result.success("获取系统统计数据成功", stats);
            
        } catch (Exception e) {
            log.error("获取系统统计数据异常: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取统计数据失败");
        }
    }

    /**
     * 修改管理员密码
     */
    @PostMapping("/change-password")
    @ApiOperation(value = "修改管理员密码", notes = "修改当前登录管理员的密码")
    public Result<String> changePassword(
            @ApiParam(value = "旧密码", required = true) @RequestParam String oldPassword,
            @ApiParam(value = "新密码", required = true) @RequestParam String newPassword
    ) {
        log.info("修改管理员密码请求");
        
        // 参数验证
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error(ResultCode.PARAM_ERROR, "新密码长度必须为6-20位");
        }
        
        try {
            // 从SecurityContext获取当前认证信息
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Result.error(ResultCode.UNAUTHORIZED, "未登录或认证已过期");
            }
            
            String username = authentication.getName();
            Admin admin = adminService.findByUsername(username);
            
            if (admin == null) {
                return Result.error(ResultCode.NOT_FOUND, "管理员不存在");
            }
            
            // 修改密码
            boolean success = adminService.changePassword(admin.getId(), oldPassword, newPassword);
            
            if (success) {
                log.info("管理员密码修改成功: username={}, adminId={}", username, admin.getId());
                return Result.success("密码修改成功");
            } else {
                log.warn("管理员密码修改失败: username={}, adminId={}", username, admin.getId());
                return Result.error(ResultCode.UNAUTHORIZED, "旧密码错误");
            }
            
        } catch (Exception e) {
            log.error("修改管理员密码异常: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "密码修改失败，请稍后重试");
        }
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    @ApiOperation(value = "管理员登出", notes = "管理员用户登出系统")
    public Result<String> adminLogout() {
        log.info("管理员登出请求");
        
        try {
            // 从SecurityContext获取当前认证信息
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                Admin admin = adminService.findByUsername(username);
                
                if (admin != null) {
                    log.info("管理员登出成功: username={}, adminId={}", username, admin.getId());
                } else {
                    log.info("管理员登出: username={}", username);
                }
                
                // 清除SecurityContext认证信息
                SecurityContextHolder.clearContext();
            }
            
            // JWT是无状态的，登出主要在前端移除token
            // 如果需要服务端登出，可以考虑实现token黑名单机制
            // 这里只记录日志并返回成功
            
            return Result.success("登出成功");
            
        } catch (Exception e) {
            log.error("管理员登出异常: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "登出失败");
        }
    }
}