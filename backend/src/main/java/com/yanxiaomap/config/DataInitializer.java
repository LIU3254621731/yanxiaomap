package com.yanxiaomap.config;

import com.yanxiaomap.entity.Admin;
import com.yanxiaomap.mapper.AdminMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(AdminMapper adminMapper, BCryptPasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initDefaultAdmin();
    }

    private void initDefaultAdmin() {
        try {
            long count = adminMapper.selectCount(null);
            if (count == 0) {
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@yanxiaomap.com");
                admin.setStatus(1);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                adminMapper.insert(admin);
                log.info("默认管理员已创建（用户名: admin, 密码: admin123）");
            } else {
                log.info("管理员已存在，跳过初始化");
            }
        } catch (Exception e) {
            log.warn("初始化管理员失败（可能表尚未创建）: {}", e.getMessage());
        }
    }
}
