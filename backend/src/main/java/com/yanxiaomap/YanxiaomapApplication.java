package com.yanxiaomap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 考研院校地图择校网站后端应用启动类
 * 主启动类，负责启动Spring Boot应用
 */
@SpringBootApplication
@EnableCaching // 启用缓存
@EnableAsync   // 启用异步处理
public class YanxiaomapApplication {

    public static void main(String[] args) {
        SpringApplication.run(YanxiaomapApplication.class, args);
    }
}