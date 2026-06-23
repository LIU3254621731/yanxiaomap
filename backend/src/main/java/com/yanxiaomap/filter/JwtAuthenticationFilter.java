package com.yanxiaomap.filter;

import com.yanxiaomap.util.JwtUtil;
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
 * JWT认证过滤器
 * 拦截所有请求，解析JWT Token并设置认证信息
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    // Token前缀
    private static final String TOKEN_PREFIX = "Bearer ";
    // Token请求头名称
    private static final String AUTHORIZATION_HEADER = "Authorization";

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
                // 解析Token，获取用户名
                String username = jwtUtil.getUsernameFromToken(token);
                
                // 验证Token有效性
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtUtil.validateToken(token, userDetails)) {
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
                    } else {
                        log.warn("JWT Token验证失败: token={}, username={}, uri={}", token.substring(0, Math.min(token.length(), 20)), username, requestURI);
                    }
                }
            } else {
                log.debug("请求未携带Token: uri={}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器异常: {}", e.getMessage(), e);
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
            requestURI.startsWith("/api/schools/search") ||
            requestURI.matches("/api/schools/\\d+/majors/\\d+") ||
            requestURI.startsWith("/api/compare") ||
            requestURI.startsWith("/api/auth/")) {
            return true;
        }
        
        // 静态资源
        if (requestURI.startsWith("/static/")) {
            return true;
        }
        
        return false;
    }
}