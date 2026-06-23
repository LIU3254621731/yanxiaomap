package com.yanxiaomap.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * XSS防护过滤器
 * 功能：
 * 1. 过滤危险HTML标签和属性
 * 2. 防止XSS攻击
 * 3. 敏感词过滤
 * 4. 请求参数清理
 */
@Slf4j
@Component
@Order(2) // 在限流过滤器之后执行
public class XssFilter implements Filter {

    // 是否启用XSS过滤
    @Value("${xss.filter.enabled:true}")
    private boolean enabled;

    // 过滤模式：escape(转义) / strip(删除)
    @Value("${xss.filter.mode:escape}")
    private String filterMode;

    // 危险HTML标签列表
    @Value("${xss.filter.html-tags:script,iframe,frame,object,embed,applet}")
    private List<String> dangerousHtmlTags;

    // 危险属性列表
    @Value("${xss.filter.dangerous-attributes:onclick,onload,onerror,onmouseover,javascript:}")
    private List<String> dangerousAttributes;

    // 敏感词文件路径
    @Value("${xss.sensitive-words-file:classpath:security/sensitive-words.txt}")
    private String sensitiveWordsFile;

    // 敏感词列表
    private Set<String> sensitiveWords = new HashSet<>();

    // 危险HTML标签正则模式
    private Pattern dangerousHtmlPattern;

    // 危险属性正则模式
    private Pattern dangerousAttributePattern;

    // 需要过滤的请求参数名称（支持通配符）
    private static final List<String> FILTERED_PARAM_NAMES = Arrays.asList(
            "content", "comment", "description", "message", "title", "name",
            "search", "query", "keyword", "text", "value", "input"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XSS防护过滤器初始化");
        log.info("XSS过滤配置 - 启用: {}, 模式: {}", enabled, filterMode);
        
        if (enabled) {
            // 初始化危险HTML标签正则
            initDangerousPatterns();
            
            // 加载敏感词
            loadSensitiveWords();
            
            log.info("XSS过滤器初始化完成，已加载 {} 个敏感词", sensitiveWords.size());
        } else {
            log.warn("XSS防护过滤器已禁用，系统可能存在XSS安全风险");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 检查是否为白名单路径（不进行XSS过滤）
        if (isExemptPath(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        
        try {
            // 包装请求对象，进行XSS过滤
            XssRequestWrapper wrappedRequest = new XssRequestWrapper(httpRequest);
            
            // 记录过滤日志（仅记录可疑请求）
            logSuspiciousRequest(httpRequest, wrappedRequest);
            
            chain.doFilter(wrappedRequest, response);
            
        } catch (Exception e) {
            log.error("XSS过滤处理异常: {}", e.getMessage(), e);
            // 发生异常时使用原始请求继续处理
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        log.info("XSS防护过滤器销毁");
    }

    /**
     * 初始化危险模式正则
     */
    private void initDangerousPatterns() {
        // 构建危险HTML标签正则
        if (dangerousHtmlTags != null && !dangerousHtmlTags.isEmpty()) {
            StringBuilder htmlPattern = new StringBuilder();
            htmlPattern.append("(?i)<\\s*(");
            
            for (int i = 0; i < dangerousHtmlTags.size(); i++) {
                if (i > 0) htmlPattern.append("|");
                htmlPattern.append(dangerousHtmlTags.get(i));
            }
            
            htmlPattern.append(")(\\s+|>|\\s+[^>]*>)");
            dangerousHtmlPattern = Pattern.compile(htmlPattern.toString());
            log.debug("危险HTML标签正则: {}", htmlPattern);
        }
        
        // 构建危险属性正则
        if (dangerousAttributes != null && !dangerousAttributes.isEmpty()) {
            StringBuilder attrPattern = new StringBuilder();
            attrPattern.append("(?i)(");
            
            for (int i = 0; i < dangerousAttributes.size(); i++) {
                if (i > 0) attrPattern.append("|");
                // 对正则特殊字符进行转义
                String escapedAttr = Pattern.quote(dangerousAttributes.get(i));
                attrPattern.append(escapedAttr);
            }
            
            attrPattern.append(")\\s*=");
            dangerousAttributePattern = Pattern.compile(attrPattern.toString());
            log.debug("危险属性正则: {}", attrPattern);
        }
    }

    /**
     * 加载敏感词
     */
    private void loadSensitiveWords() {
        try {
            // 从文件中加载敏感词
            // 这里简化实现，实际应该从文件读取
            // 添加一些默认敏感词
            sensitiveWords.addAll(Arrays.asList(
                    "<script", "javascript:", "onclick", "onload", "onerror",
                    "eval(", "alert(", "document.cookie", "window.location",
                    "<iframe", "<object", "<embed", "<applet",
                    "select", "union", "or ", "and ", "exec(", "execute(",
                    "xp_", "sp_", "drop ", "delete ", "update ", "insert ",
                    "file://", "ftp://", "gopher://", "data:",
                    "../", "..\\", "/etc/passwd", "/etc/shadow",
                    "<?php", "<%@", "<%=", "${", "#{"
            ));
            
            log.debug("已加载 {} 个默认敏感词", sensitiveWords.size());
        } catch (Exception e) {
            log.error("加载敏感词异常: {}", e.getMessage());
        }
    }

    /**
     * 检查是否为豁免路径
     */
    private boolean isExemptPath(String requestURI) {
        // 文件上传接口（文件内容不进行XSS过滤）
        if (requestURI.startsWith("/api/upload") || 
            requestURI.startsWith("/api/file/upload")) {
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
        
        return false;
    }

    /**
     * 记录可疑请求
     */
    private void logSuspiciousRequest(HttpServletRequest originalRequest, XssRequestWrapper wrappedRequest) {
        try {
            // 获取原始参数
            Map<String, String[]> originalParams = originalRequest.getParameterMap();
            Map<String, String[]> filteredParams = wrappedRequest.getParameterMap();
            
            boolean hasSuspiciousContent = false;
            
            // 检查每个参数是否被修改
            for (Map.Entry<String, String[]> entry : originalParams.entrySet()) {
                String paramName = entry.getKey();
                String[] originalValues = entry.getValue();
                String[] filteredValues = filteredParams.get(paramName);
                
                if (originalValues != null && filteredValues != null && 
                    originalValues.length == filteredValues.length) {
                    
                    for (int i = 0; i < originalValues.length; i++) {
                        String originalValue = originalValues[i];
                        String filteredValue = filteredValues[i];
                        
                        if (originalValue != null && filteredValue != null && 
                            !originalValue.equals(filteredValue)) {
                            // 参数值被修改，可能存在XSS攻击
                            hasSuspiciousContent = true;
                            
                            log.warn("XSS过滤检测到可疑内容: param={}, original={}, filtered={}, ip={}, uri={}",
                                    paramName,
                                    originalValue.length() > 100 ? originalValue.substring(0, 100) + "..." : originalValue,
                                    filteredValue.length() > 100 ? filteredValue.substring(0, 100) + "..." : filteredValue,
                                    getClientIp(originalRequest),
                                    originalRequest.getRequestURI());
                        }
                    }
                }
            }
            
            if (hasSuspiciousContent) {
                // 增加安全事件计数
                incrementSecurityEventCount();
            }
            
        } catch (Exception e) {
            log.debug("记录可疑请求异常: {}", e.getMessage());
        }
    }

    /**
     * 增加安全事件计数
     */
    private void incrementSecurityEventCount() {
        // 这里可以集成到安全监控系统
        log.debug("XSS安全事件计数+1");
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
     * 清理字符串中的XSS危险内容
     */
    public String cleanXss(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        String cleaned = input;
        
        // 1. 敏感词检测
        for (String sensitiveWord : sensitiveWords) {
            if (cleaned.toLowerCase().contains(sensitiveWord.toLowerCase())) {
                log.debug("检测到敏感词: {}, 内容: {}", sensitiveWord, 
                        cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned);
            }
        }
        
        // 2. 根据过滤模式进行处理
        if ("escape".equals(filterMode)) {
            // 转义模式：将危险字符转义为HTML实体
            cleaned = escapeHtml(cleaned);
        } else if ("strip".equals(filterMode)) {
            // 删除模式：删除危险内容
            cleaned = stripDangerousContent(cleaned);
        }
        
        // 3. 额外安全处理：防止SQL注入
        cleaned = cleaned.replace("'", "''");
        cleaned = cleaned.replace("\\", "\\\\");
        
        return cleaned;
    }

    /**
     * 转义HTML特殊字符
     */
    private String escapeHtml(String input) {
        if (input == null) return null;
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;")
                   .replace("(", "&#40;")
                   .replace(")", "&#41;")
                   .replace("`", "&#96;");
    }

    /**
     * 删除危险内容
     */
    private String stripDangerousContent(String input) {
        if (input == null) return null;
        
        String result = input;
        
        // 删除危险HTML标签
        if (dangerousHtmlPattern != null) {
            result = dangerousHtmlPattern.matcher(result).replaceAll("");
        }
        
        // 删除危险属性
        if (dangerousAttributePattern != null) {
            result = dangerousAttributePattern.matcher(result).replaceAll("");
        }
        
        // 删除JavaScript协议
        result = result.replaceAll("(?i)javascript\\s*:", "");
        result = result.replaceAll("(?i)vbscript\\s*:", "");
        result = result.replaceAll("(?i)data\\s*:", "");
        
        // 删除事件处理器
        result = result.replaceAll("(?i)on\\w+\\s*=", "");
        
        // 删除注释
        result = result.replaceAll("<!--.*?-->", "");
        result = result.replaceAll("/\\*.*?\\*/", "");
        
        // 删除脚本标签内容
        result = result.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        return result;
    }

    /**
     * 判断是否需要过滤该参数
     */
    private boolean shouldFilterParameter(String paramName) {
        if (paramName == null) return false;
        
        String lowerParamName = paramName.toLowerCase();
        
        // 检查是否在过滤列表中
        for (String pattern : FILTERED_PARAM_NAMES) {
            if (pattern.equals("*") || lowerParamName.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * XSS请求包装类
     */
    private class XssRequestWrapper extends HttpServletRequestWrapper {
        
        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            
            // 只过滤需要过滤的参数
            if (shouldFilterParameter(name) && value != null) {
                return cleanXss(value);
            }
            
            return value;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            
            if (!shouldFilterParameter(name) || values == null) {
                return values;
            }
            
            String[] cleanedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanedValues[i] = cleanXss(values[i]);
            }
            
            return cleanedValues;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> paramMap = super.getParameterMap();
            Map<String, String[]> cleanedMap = new HashMap<>();
            
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                String paramName = entry.getKey();
                String[] values = entry.getValue();
                
                if (!shouldFilterParameter(paramName) || values == null) {
                    cleanedMap.put(paramName, values);
                    continue;
                }
                
                String[] cleanedValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    cleanedValues[i] = cleanXss(values[i]);
                }
                
                cleanedMap.put(paramName, cleanedValues);
            }
            
            return cleanedMap;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            
            // 对某些请求头进行过滤
            if (value != null && ("referer".equalsIgnoreCase(name) || 
                                  "user-agent".equalsIgnoreCase(name))) {
                return cleanXss(value);
            }
            
            return value;
        }
    }

    /**
     * 手动清理字符串（供外部调用）
     */
    public String clean(String input) {
        return cleanXss(input);
    }

    /**
     * 检查字符串是否包含XSS危险内容
     */
    public boolean containsXss(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        
        // 检查危险HTML标签
        if (dangerousHtmlPattern != null && dangerousHtmlPattern.matcher(input).find()) {
            return true;
        }
        
        // 检查危险属性
        if (dangerousAttributePattern != null && dangerousAttributePattern.matcher(input).find()) {
            return true;
        }
        
        // 检查JavaScript协议
        if (input.matches("(?i).*javascript\\s*:.*") ||
            input.matches("(?i).*vbscript\\s*:.*") ||
            input.matches("(?i).*data\\s*:.*")) {
            return true;
        }
        
        // 检查敏感词
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : sensitiveWords) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}