package com.yanxiaomap.security;

import com.yanxiaomap.entity.Admin;
import com.yanxiaomap.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 管理员用户详情服务
 * Spring Security的UserDetailsService实现，用于从数据库加载管理员用户信息
 */
@Slf4j
@Service
public class AdminUserDetailsService implements UserDetailsService {

    @Autowired
    private AdminService adminService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载管理员用户详情: username={}", username);
        
        // 从数据库查询管理员信息
        Admin admin = adminService.findByUsername(username);
        
        if (admin == null) {
            log.warn("管理员账号不存在: {}", username);
            throw new UsernameNotFoundException("管理员账号不存在: " + username);
        }
        
        // 检查账户状态
        if (admin.getStatus() == 0) {
            log.warn("管理员账号已禁用: {}", username);
            throw new UsernameNotFoundException("管理员账号已禁用: " + username);
        }
        
        // 检查账户是否被锁定
        if (adminService.isAdminLocked(admin.getId())) {
            log.warn("管理员账号已锁定: {}", username);
            throw new UsernameNotFoundException("管理员账号已锁定: " + username);
        }
        
        // 构建权限列表
        Collection<GrantedAuthority> authorities = buildAuthorities(admin);
        
        // 创建Spring Security UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                admin.getUsername(),
                admin.getPassword(),
                true, // 账户是否启用
                true, // 账户是否未过期
                true, // 凭证是否未过期
                true, // 账户是否未锁定
                authorities
        );
    }

    /**
     * 构建管理员权限列表
     */
    private Collection<GrantedAuthority> buildAuthorities(Admin admin) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 所有管理员都拥有ROLE_ADMIN角色
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        // 可以根据管理员的属性添加更多权限
        // 例如：如果管理员有特定权限字段，可以在这里解析并添加
        
        // 添加基本权限
        authorities.add(new SimpleGrantedAuthority("user:manage"));
        authorities.add(new SimpleGrantedAuthority("data:manage"));
        authorities.add(new SimpleGrantedAuthority("system:config"));
        
        log.debug("管理员权限: username={}, authorities={}", admin.getUsername(), authorities);
        return authorities;
    }

    /**
     * 根据管理员ID加载用户详情
     */
    public UserDetails loadUserById(Integer adminId) {
        Admin admin = adminService.getById(adminId);
        
        if (admin == null) {
            throw new UsernameNotFoundException("管理员不存在: " + adminId);
        }
        
        // 检查账户状态
        if (admin.getStatus() == 0) {
            throw new UsernameNotFoundException("管理员账号已禁用: " + adminId);
        }
        
        // 检查账户是否被锁定
        if (adminService.isAdminLocked(admin.getId())) {
            throw new UsernameNotFoundException("管理员账号已锁定: " + adminId);
        }
        
        // 构建权限列表
        Collection<GrantedAuthority> authorities = buildAuthorities(admin);
        
        // 创建Spring Security UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                admin.getUsername(),
                admin.getPassword(),
                true, // 账户是否启用
                true, // 账户是否未过期
                true, // 凭证是否未过期
                true, // 账户是否未锁定
                authorities
        );
    }
}