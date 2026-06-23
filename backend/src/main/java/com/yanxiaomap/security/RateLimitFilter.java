package com.yanxiaomap.security;

import com.yanxiaomap.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流过滤器
 * 功能：
 * 1. 基于IP的接口限流
 * 2. 用户端接口限制10次/分钟/IP
 * 3. 管理端接口限制30次/分钟/IP
 * 4. 限流异常处理，返回友好提示
 */
@Slf4j
@Component
@Order(1) // 在JWT过滤器之前执行
public class RateLimitFilter implements Filter {

    @Autowired
    private RedisUtil redisUtil;

    // 用户端接口限流配置
    @Value("${rate-limit.user:10}")
    private int userRateLimit;

    // 管理端接口限流配置
    @Value("${rate-limit.admin:30}")
    private int adminRateLimit;

    // 公共接口限流配置
    @Value("${rate-limit.public:60}")
    private int publicRateLimit;

    // 限流时间窗口（分钟）
    private static final int TIME_WINDOW_MINUTES = 1;

    // 限流键前缀
    private static final String RATE_LIMIT_PREFIX = "rate:limit:";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("接口限流过滤器初始化完成");
        log.info("限流配置 - 用户端: {}/分钟, 管理端: {}/分钟, 公共接口: {}/分钟", 
                userRateLimit, adminRateLimit, publicRateLimit);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String clientIp = getClientIp(httpRequest);
        
        // 检查是否为白名单路径（不进行限流）
        if (isExemptPath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        
        // 根据接口类型确定限流阈值
        int rateLimit = getRateLimitForPath(requestURI);
        String rateLimitKey = buildRateLimitKey(clientIp, requestURI);
        
        try {
            // 检查是否超过限流阈值
            if (isRateLimited(rateLimitKey, rateLimit)) {
                log.warn("接口访问频率超限: ip={}, uri={}, limit={}/分钟", 
                        clientIp, requestURI, rateLimit);
                
                sendRateLimitResponse(httpResponse, rateLimit);
                return;
            }
            
            // 增加访问计数
            incrementRateLimitCounter(rateLimitKey);
            
            // 将限流信息添加到响应头
            addRateLimitHeaders(httpResponse, rateLimitKey, rateLimit);
            
        } catch (Exception e) {
            log.error("接口限流处理异常: {}", e.getMessage(), e);
            // 发生异常时放行，避免影响正常服务
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("接口限流过滤器销毁");
    }

    /**
     * 检查是否为豁免路径（不进行限流）
     */
    private boolean isExemptPath(String requestURI) {
        // 健康检查接口
        if (requestURI.equals("/actuator/health")) {
            return true;
        }
        
        // Swagger文档
        if (requestURI.startsWith("/swagger-ui") || 
            requestURI.startsWith("/swagger-resources") ||
            requestURI.startsWith("/v3/api-docs") ||
            requestURI.startsWith("/webjars")) {
            return true;
        }
        
        // 静态资源
        if (requestURI.startsWith("/static/")) {
            return true;
        }
        
        // 合规信息接口
        if (requestURI.startsWith("/api/compliance/")) {
            return true;
        }
        
        return false;
    }

    /**
     * 根据接口路径确定限流阈值
     */
    private int getRateLimitForPath(String requestURI) {
        // 管理端接口
        if (requestURI.startsWith("/api/admin/")) {
            return adminRateLimit;
        }
        
        // 用户端接口（需要认证的接口）
        if (requestURI.startsWith("/api/user/") || 
            requestURI.startsWith("/api/data/") ||
            requestURI.startsWith("/api/system/")) {
            return userRateLimit;
        }
        
        // 公共接口（地图、搜索等）
        if (requestURI.startsWith("/api/map/") ||
            requestURI.startsWith("/api/search/") ||
            requestURI.startsWith("/api/details/") ||
            requestURI.startsWith("/api/compare/")) {
            return publicRateLimit;
        }
        
        // 默认使用公共接口限流
        return publicRateLimit;
    }

    /**
     * 构建限流键
     */
    private String buildRateLimitKey(String clientIp, String requestURI) {
        // 提取接口主要路径（避免具体参数影响）
        String path = normalizePath(requestURI);
        
        // 构建限流键：前缀 + IP + 路径 + 时间窗口
        long timeWindow = System.currentTimeMillis() / (TIME_WINDOW_MINUTES * 60 * 1000);
        return RATE_LIMIT_PREFIX + clientIp + ":" + path + ":" + timeWindow;
    }

    /**
     * 规范化路径（移除参数和ID）
     */
    private String normalizePath(String requestURI) {
        // 移除路径中的数字ID
        String normalized = requestURI.replaceAll("/\\d+", "/{id}");
        
        // 如果路径仍然过长，只取前两部分
        String[] parts = normalized.split("/");
        if (parts.length > 4) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(4, parts.length); i++) {
                if (StringUtils.hasText(parts[i])) {
                    sb.append("/").append(parts[i]);
                }
            }
            return sb.toString();
        }
        
        return normalized;
    }

    /**
     * 检查是否超过限流阈值
     */
    private boolean isRateLimited(String rateLimitKey, int rateLimit) {
        try {
            Object countObj = redisUtil.get(rateLimitKey);
            String countStr = countObj != null ? countObj.toString() : null;
            if (countStr == null) {
                return false;
            }
            
            int count = Integer.parseInt(countStr);
            return count >= rateLimit;
        } catch (Exception e) {
            log.error("检查限流状态异常: {}", e.getMessage());
            return false; // 发生异常时放行
        }
    }

    /**
     * 增加访问计数
     */
    private void incrementRateLimitCounter(String rateLimitKey) {
        try {
            // 使用Redis递增计数，如果键不存在则创建并设置过期时间
            Long count = redisUtil.incr(rateLimitKey, 1);
            
            // 如果是第一次访问，设置过期时间
            if (count != null && count == 1) {
                redisUtil.expire(rateLimitKey, TIME_WINDOW_MINUTES * 60 + 10); // 多加10秒避免边界问题
            }
            
            log.debug("接口访问计数: key={}, count={}", rateLimitKey, count);
        } catch (Exception e) {
            log.error("增加访问计数异常: {}", e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 对于多个IP的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        // 如果IP为空或为本地地址，使用默认值
        if (ip == null || ip.isEmpty() || "0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        return ip;
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response, int rateLimit) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(429); // Too Many Requests
        response.setHeader("Retry-After", String.valueOf(TIME_WINDOW_MINUTES * 60));
        
        String message = String.format("请求过于频繁，当前限制为 %d 次/分钟，请稍后再试", rateLimit);
        String jsonResponse = String.format(
                "{\"success\":false,\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"%s\",\"rateLimit\":%d,\"windowMinutes\":%d}",
                message, rateLimit, TIME_WINDOW_MINUTES);
        
        response.getWriter().write(jsonResponse);
    }

    /**
     * 添加限流信息到响应头
     */
    private void addRateLimitHeaders(HttpServletResponse response, String rateLimitKey, int rateLimit) {
        try {
            Object countObj = redisUtil.get(rateLimitKey);
            String countStr = countObj != null ? countObj.toString() : null;
            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                int remaining = Math.max(0, rateLimit - count);
                long ttl = redisUtil.getExpire(rateLimitKey);
                
                response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Reset", String.valueOf(ttl));
            }
        } catch (Exception e) {
            log.debug("添加限流响应头异常: {}", e.getMessage());
        }
    }

    /**
     * 清除限流计数（供外部调用）
     */
    public void clearRateLimit(String clientIp, String requestURI) {
        try {
            String rateLimitKey = buildRateLimitKey(clientIp, requestURI);
            redisUtil.delete(rateLimitKey);
            log.info("限流计数已清除: ip={}, uri={}", clientIp, requestURI);
        } catch (Exception e) {
            log.error("清除限流计数异常: {}", e.getMessage());
        }
    }

    /**
     * 获取当前限流状态（供监控使用）
     */
    public RateLimitStatus getRateLimitStatus(String clientIp, String requestURI) {
        try {
            String rateLimitKey = buildRateLimitKey(clientIp, requestURI);
            Object countObj = redisUtil.get(rateLimitKey);
            String countStr = countObj != null ? countObj.toString() : null;
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            int rateLimit = getRateLimitForPath(requestURI);
            int remaining = Math.max(0, rateLimit - count);
            long ttl = redisUtil.getExpire(rateLimitKey);
            
            return new RateLimitStatus(clientIp, requestURI, count, rateLimit, remaining, ttl);
        } catch (Exception e) {
            log.error("获取限流状态异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 限流状态内部类
     */
    public static class RateLimitStatus {
        private String clientIp;
        private String requestUri;
        private int currentCount;
        private int rateLimit;
        private int remaining;
        private long ttlSeconds;

        public RateLimitStatus(String clientIp, String requestUri, int currentCount, 
                              int rateLimit, int remaining, long ttlSeconds) {
            this.clientIp = clientIp;
            this.requestUri = requestUri;
            this.currentCount = currentCount;
            this.rateLimit = rateLimit;
            this.remaining = remaining;
            this.ttlSeconds = ttlSeconds;
        }

        public String getClientIp() { return clientIp; }
        public String getRequestUri() { return requestUri; }
        public int getCurrentCount() { return currentCount; }
        public int getRateLimit() { return rateLimit; }
        public int getRemaining() { return remaining; }
        public long getTtlSeconds() { return ttlSeconds; }
    }
}