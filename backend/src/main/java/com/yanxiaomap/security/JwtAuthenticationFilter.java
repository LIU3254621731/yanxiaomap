package com.yanxiaomap.security;

import com.yanxiaomap.util.JwtUtil;
import com.yanxiaomap.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器（增强版）
 * 拦截所有请求，解析JWT Token并设置认证信息
 * 增强功能：
 * 1. Token黑名单检查（支持主动注销）
 * 2. Token绑定IP和User-Agent（防止盗用）
 * 3. Token刷新机制
 * 4. 安全日志记录
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RedisUtil redisUtil;

    // Token前缀
    private static final String TOKEN_PREFIX = "Bearer ";
    // Token请求头名称
    private static final String AUTHORIZATION_HEADER = "Authorization";
    // Token刷新头名称
    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    // Token黑名单前缀
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    // Token绑定信息前缀
    private static final String TOKEN_BINDING_PREFIX = "token:binding:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 获取请求路径
        String requestURI = request.getRequestURI();
        
        // 白名单路径直接放行
        if (isWhiteListPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 从请求头中获取Token
            String token = resolveToken(request);
            
            if (StringUtils.hasText(token)) {
                // 1. 检查Token是否在黑名单中
                if (isTokenBlacklisted(token)) {
                    log.warn("Token已被列入黑名单，拒绝访问: token={}, uri={}", 
                            token.substring(0, Math.min(token.length(), 20)), requestURI);
                    sendUnauthorizedResponse(response, "Token已失效，请重新登录");
                    return;
                }
                
                // 2. 解析Token，获取用户名
                String username = jwtUtil.getUsernameFromToken(token);
                
                // 3. 验证Token有效性
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtUtil.validateToken(token, userDetails)) {
                        // 4. 检查Token绑定信息（IP和User-Agent）
                        if (validateTokenBinding(token, request)) {
                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            // 设置认证信息到SecurityContext
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("JWT认证成功: username={}, uri={}", username, requestURI);
                            
                            // 5. 检查Token是否需要刷新（剩余有效期小于30分钟）
                            if (shouldRefreshToken(token)) {
                                String newToken = jwtUtil.refreshToken(token);
                                // 将新Token设置到响应头中
                                response.setHeader("X-New-Token", newToken);
                                log.debug("Token已刷新: username={}, oldToken={}, newToken={}", 
                                        username, 
                                        token.substring(0, Math.min(token.length(), 10)),
                                        newToken.substring(0, Math.min(newToken.length(), 10)));
                                
                                // 更新Token绑定信息
                                updateTokenBinding(newToken, request);
                            }
                        } else {
                            log.warn("Token绑定验证失败，可能被盗用: username={}, token={}, uri={}", 
                                    username, token.substring(0, Math.min(token.length(), 20)), requestURI);
                            sendUnauthorizedResponse(response, "安全验证失败，请重新登录");
                            return;
                        }
                    } else {
                        log.warn("JWT Token验证失败: token={}, username={}, uri={}", 
                                token.substring(0, Math.min(token.length(), 20)), username, requestURI);
                    }
                }
            } else {
                log.debug("请求未携带Token: uri={}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器异常: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "认证失败，请重新登录");
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中解析Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        
        // 也支持从参数中获取Token
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }

    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + token.hashCode();
            return redisUtil.hasKey(key);
        } catch (Exception e) {
            log.error("检查Token黑名单异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token绑定信息（IP和User-Agent）
     */
    private boolean validateTokenBinding(String token, HttpServletRequest request) {
        try {
            String key = TOKEN_BINDING_PREFIX + token.hashCode();
            Object bindingObj = redisUtil.get(key);
            String bindingInfo = bindingObj != null ? bindingObj.toString() : null;
            
            if (bindingInfo == null) {
                // 如果Redis中没有绑定信息，则创建新的绑定信息
                updateTokenBinding(token, request);
                return true;
            }
            
            // 解析绑定信息（格式：ip|userAgent|timestamp）
            String[] parts = bindingInfo.split("\\|");
            if (parts.length < 2) {
                return true; // 绑定信息格式错误，允许通过（安全宽松）
            }
            
            String storedIp = parts[0];
            String storedUserAgent = parts[1];
            String currentIp = getClientIp(request);
            String currentUserAgent = request.getHeader("User-Agent");
            
            // 验证IP和User-Agent是否匹配
            boolean ipMatches = storedIp.equals(currentIp);
            boolean userAgentMatches = storedUserAgent.equals(currentUserAgent);
            
            log.debug("Token绑定验证: ipMatches={}, userAgentMatches={}, storedIp={}, currentIp={}", 
                    ipMatches, userAgentMatches, storedIp, currentIp);
            
            // 如果配置了严格模式，需要IP和User-Agent都匹配
            // 这里使用宽松模式：只要IP匹配即可
            return ipMatches;
            
        } catch (Exception e) {
            log.error("验证Token绑定异常: {}", e.getMessage());
            return true; // 发生异常时允许通过，避免影响正常使用
        }
    }

    /**
     * 更新Token绑定信息
     */
    private void updateTokenBinding(String token, HttpServletRequest request) {
        try {
            String key = TOKEN_BINDING_PREFIX + token.hashCode();
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            long timestamp = System.currentTimeMillis();
            
            String bindingInfo = ip + "|" + userAgent + "|" + timestamp;
            // Token绑定信息保存24小时
            redisUtil.set(key, bindingInfo, 24 * 60 * 60);
            
            log.debug("Token绑定信息已更新: token={}, ip={}", 
                    token.substring(0, Math.min(token.length(), 10)), ip);
        } catch (Exception e) {
            log.error("更新Token绑定信息异常: {}", e.getMessage());
        }
    }

    /**
     * 检查Token是否需要刷新
     */
    private boolean shouldRefreshToken(String token) {
        try {
            long expiration = jwtUtil.getExpirationDateFromToken(token).getTime();
            long current = System.currentTimeMillis();
            long remaining = expiration - current;
            
            // 剩余有效期小于30分钟（1800000毫秒）时刷新
            return remaining > 0 && remaining < 30 * 60 * 1000;
        } catch (Exception e) {
            log.error("检查Token刷新条件异常: {}", e.getMessage());
            return false;
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
        
        return ip;
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}");
    }

    /**
     * 检查是否为白名单路径
     */
    private boolean isWhiteListPath(String requestURI) {
        // Swagger文档
        if (requestURI.startsWith("/swagger-ui") || 
            requestURI.startsWith("/swagger-resources") ||
            requestURI.startsWith("/v3/api-docs") ||
            requestURI.startsWith("/webjars")) {
            return true;
        }
        
        // 健康检查
        if (requestURI.equals("/actuator/health")) {
            return true;
        }
        
        // 用户端公开接口
        if (requestURI.startsWith("/api/map/") ||
            requestURI.startsWith("/api/search/") ||
            requestURI.matches("/api/details/.*") ||
            requestURI.startsWith("/api/compare") ||
            requestURI.startsWith("/api/auth/")) {
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
     * 将Token加入黑名单（供外部调用）
     */
    public void addToBlacklist(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + token.hashCode();
            // 黑名单保存24小时（与Token有效期一致）
            redisUtil.set(key, "blacklisted", 24 * 60 * 60);
            log.info("Token已加入黑名单: token={}", token.substring(0, Math.min(token.length(), 20)));
        } catch (Exception e) {
            log.error("将Token加入黑名单异常: {}", e.getMessage());
        }
    }

    /**
     * 清除Token绑定信息（供外部调用）
     */
    public void clearTokenBinding(String token) {
        try {
            String key = TOKEN_BINDING_PREFIX + token.hashCode();
            redisUtil.delete(key);
            log.debug("Token绑定信息已清除: token={}", token.substring(0, Math.min(token.length(), 10)));
        } catch (Exception e) {
            log.error("清除Token绑定信息异常: {}", e.getMessage());
        }
    }
}