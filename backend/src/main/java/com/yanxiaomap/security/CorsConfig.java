package com.yanxiaomap.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * CORS安全配置
 * 功能：
 * 1. 配置跨域资源共享策略
 * 2. 限制允许的源、方法、头部
 * 3. 支持凭证传递
 * 4. 安全优化配置
 */
@Configuration
public class CorsConfig {

    // 允许的源（逗号分隔）
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    // 允许的方法（逗号分隔）
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    // 允许的头部（逗号分隔）
    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    // 是否允许凭证
    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    // 预检请求缓存时间（秒）
    @Value("${cors.max-age:3600}")
    private long maxAge;

    // 暴露的响应头
    @Value("${cors.exposed-headers:}")
    private String exposedHeaders;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 设置允许的源
        if ("*".equals(allowedOrigins)) {
            configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        } else {
            String[] origins = allowedOrigins.split(",");
            configuration.setAllowedOrigins(Arrays.asList(origins));
        }
        
        // 设置允许的方法
        String[] methods = allowedMethods.split(",");
        configuration.setAllowedMethods(Arrays.asList(methods));
        
        // 设置允许的头部
        if ("*".equals(allowedHeaders)) {
            configuration.setAllowedHeaders(Collections.singletonList("*"));
        } else {
            String[] headers = allowedHeaders.split(",");
            configuration.setAllowedHeaders(Arrays.asList(headers));
        }
        
        // 设置是否允许凭证
        configuration.setAllowCredentials(allowCredentials);
        
        // 设置预检请求缓存时间
        configuration.setMaxAge(maxAge);
        
        // 设置暴露的响应头
        if (exposedHeaders != null && !exposedHeaders.trim().isEmpty()) {
            String[] headers = exposedHeaders.split(",");
            configuration.setExposedHeaders(Arrays.asList(headers));
        }
        
        // 安全增强配置
        enhanceSecurityConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // 对所有API路径应用CORS配置
        source.registerCorsConfiguration("/api/**", configuration);
        
        // 对Swagger路径应用CORS配置
        source.registerCorsConfiguration("/swagger-ui/**", configuration);
        source.registerCorsConfiguration("/v3/api-docs/**", configuration);
        
        // 对静态资源路径应用CORS配置
        source.registerCorsConfiguration("/static/**", configuration);
        
        return source;
    }

    /**
     * 增强安全配置
     */
    private void enhanceSecurityConfiguration(CorsConfiguration configuration) {
        // 安全配置：在生产环境中应更严格
        
        // 1. 防止反射型跨站脚本（Reflected XSS）
        // 通过CORS限制减少攻击面
        
        // 2. 防止跨站请求伪造（CSRF）
        // CORS配置应配合CSRF保护
        
        // 3. 信息泄露防护
        // 限制暴露的头部信息
        
        // 4. 预检请求优化
        // 合理设置max-age，避免频繁预检请求
        
        // 记录CORS配置信息（生产环境应适当减少日志）
        logConfigurationInfo(configuration);
    }

    /**
     * 记录配置信息
     */
    private void logConfigurationInfo(CorsConfiguration configuration) {
        System.out.println("=== CORS安全配置信息 ===");
        System.out.println("允许的源: " + configuration.getAllowedOrigins());
        System.out.println("允许的方法: " + configuration.getAllowedMethods());
        System.out.println("允许的头部: " + configuration.getAllowedHeaders());
        System.out.println("允许凭证: " + configuration.getAllowCredentials());
        System.out.println("预检缓存时间: " + configuration.getMaxAge() + "秒");
        System.out.println("暴露的头部: " + configuration.getExposedHeaders());
        System.out.println("=========================");
    }

    /**
     * 获取安全的CORS配置（供程序化调用）
     */
    public static CorsConfiguration getSecureCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 生产环境推荐配置
        configuration.setAllowedOrigins(Arrays.asList(
                "https://yanxiaomap.com",
                "https://www.yanxiaomap.com"
        ));
        
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization", 
                "X-Requested-With", "X-CSRF-TOKEN", "X-Refresh-Token"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
                "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset",
                "X-New-Token"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1小时
        
        return configuration;
    }

    /**
     * 开发环境CORS配置（宽松）
     */
    public static CorsConfiguration getDevelopmentCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        return configuration;
    }

    /**
     * 严格CORS配置（最高安全级别）
     */
    public static CorsConfiguration getStrictCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 仅允许特定源
        configuration.setAllowedOrigins(Collections.singletonList("https://yanxiaomap.com"));
        
        // 仅允许必要的方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        
        // 仅允许必要的头部
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type", "Authorization", "X-Requested-With"
        ));
        
        // 仅暴露必要的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "X-RateLimit-Limit", "X-RateLimit-Remaining"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(1800L); // 30分钟，更频繁的预检
        
        return configuration;
    }

    /**
     * 检查CORS配置安全性
     */
    public static boolean isConfigurationSecure(CorsConfiguration configuration) {
        if (configuration == null) {
            return false;
        }
        
        // 检查是否允许所有源（不安全）
        if (configuration.getAllowedOrigins() != null && 
            configuration.getAllowedOrigins().contains("*")) {
            return false;
        }
        
        // 检查是否允许所有方法（可能不安全）
        if (configuration.getAllowedMethods() != null && 
            configuration.getAllowedMethods().contains("*")) {
            return false;
        }
        
        // 检查是否允许所有头部（不安全）
        if (configuration.getAllowedHeaders() != null && 
            configuration.getAllowedHeaders().contains("*")) {
            return false;
        }
        
        // 检查预检缓存时间是否过长（安全考虑）
        if (configuration.getMaxAge() > 86400L) { // 超过1天
            return false;
        }
        
        return true;
    }

    /**
     * 验证CORS请求（供过滤器使用）
     */
    public static boolean validateCorsRequest(String origin, String method, 
                                             CorsConfiguration configuration) {
        if (configuration == null || origin == null) {
            return false;
        }
        
        // 检查源是否允许
        boolean originAllowed = false;
        if (configuration.getAllowedOrigins() != null) {
            originAllowed = configuration.getAllowedOrigins().contains(origin);
        }
        if (!originAllowed && configuration.getAllowedOriginPatterns() != null) {
            for (String pattern : configuration.getAllowedOriginPatterns()) {
                if (pattern.equals("*") || origin.matches(pattern)) {
                    originAllowed = true;
                    break;
                }
            }
        }
        
        if (!originAllowed) {
            return false;
        }
        
        // 检查方法是否允许
        if (method != null && configuration.getAllowedMethods() != null) {
            if (!configuration.getAllowedMethods().contains(method)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 生成CORS响应头（供手动处理CORS时使用）
     */
    public static void setCorsResponseHeaders(String origin, String requestedMethod,
                                             CorsConfiguration configuration,
                                             jakarta.servlet.http.HttpServletResponse response) {
        if (configuration == null || response == null) {
            return;
        }
        
        // 设置允许的源
        if (origin != null && validateCorsRequest(origin, requestedMethod, configuration)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        
        // 设置允许的方法
        if (configuration.getAllowedMethods() != null) {
            response.setHeader("Access-Control-Allow-Methods", 
                    String.join(", ", configuration.getAllowedMethods()));
        }
        
        // 设置允许的头部
        if (configuration.getAllowedHeaders() != null) {
            response.setHeader("Access-Control-Allow-Headers", 
                    String.join(", ", configuration.getAllowedHeaders()));
        }
        
        // 设置是否允许凭证
        if (configuration.getAllowCredentials()) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        // 设置预检缓存时间
        if (configuration.getMaxAge() > 0) {
            response.setHeader("Access-Control-Max-Age", String.valueOf(configuration.getMaxAge()));
        }
        
        // 设置暴露的头部
        if (configuration.getExposedHeaders() != null && !configuration.getExposedHeaders().isEmpty()) {
            response.setHeader("Access-Control-Expose-Headers", 
                    String.join(", ", configuration.getExposedHeaders()));
        }
    }
}