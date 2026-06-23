# 安全配置指南

## 概述
本文档提供"研校地图"网站的安全配置详细指南，包括环境配置、安全组件配置、合规配置等，供开发、测试和运维人员参考。

## 1. 环境配置

### 1.1 开发环境配置

#### 1.1.1 本地开发环境
**安全要求**: 开发环境也需遵循基本安全规范

**配置步骤**:
1. 复制环境变量模板
   ```bash
   cp backend/.env.example backend/.env
   ```
2. 配置安全相关环境变量
   ```env
   # JWT配置
   JWT_SECRET=your-development-jwt-secret-key-change-in-production
   JWT_EXPIRATION=7200  # 2小时
   
   # 密码加密
   BCRYPT_SALT_ROUNDS=10
   
   # 高德地图API密钥（开发环境使用测试密钥）
   AMAP_API_KEY=test-amap-api-key
   AMAP_SECURITY_KEY=test-amap-security-key
   
   # 数据库连接（使用本地开发数据库）
   DATABASE_URL=jdbc:mysql://localhost:3306/yanxiaomap_dev
   DATABASE_USERNAME=dev_user
   DATABASE_PASSWORD=dev_password_123
   
   # Redis配置
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=
   ```
3. 启动本地安全组件
   ```bash
   # 启动MySQL（如果未运行）
   docker run --name mysql-dev -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=yanxiaomap_dev -p 3306:3306 -d mysql:8.0
   
   # 启动Redis（如果未运行）
   docker run --name redis-dev -p 6379:6379 -d redis:alpine
   ```

#### 1.1.2 安全开发工具
**必备工具**:
- **IDE插件**: SonarLint、Checkstyle
- **代码扫描**: SpotBugs、Dependency-Check
- **API测试**: Postman、Insomnia

**配置示例** (IntelliJ IDEA):
```xml
<!-- .idea/sonarlint.xml -->
<sonarlint>
  <rules>
    <rule key="java:S2068"> <!-- 硬编码密码 -->
      <severity>CRITICAL</severity>
    </rule>
    <rule key="java:S3649"> <!-- SQL注入 -->
      <severity>BLOCKER</severity>
    </rule>
  </rules>
</sonarlint>
```

### 1.2 测试环境配置

#### 1.2.1 集成测试环境
**安全配置**:
```yaml
# backend/src/test/resources/application-test.yml
security:
  jwt:
    secret: test-jwt-secret-for-integration-tests
    expiration: 3600
  password:
    encoder:
      strength: 10
  csrf:
    enabled: false  # 测试环境禁用CSRF
  cors:
    allowed-origins: http://localhost:3000
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
```

#### 1.2.2 安全测试配置
**测试数据准备**:
```java
// 安全测试数据准备示例
@Test
public void testAuthenticationSecurity() {
    // 测试密码加密
    String rawPassword = "Test@123456";
    String encodedPassword = passwordEncoder.encode(rawPassword);
    assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    
    // 测试JWT令牌
    String token = jwtUtil.generateToken("testuser");
    assertTrue(jwtUtil.validateToken(token));
    assertEquals("testuser", jwtUtil.getUsernameFromToken(token));
}
```

### 1.3 生产环境配置

#### 1.3.1 环境变量安全
**关键安全环境变量**:
```bash
# 生产环境环境变量示例（通过Kubernetes Secrets或Docker Secrets管理）
export JWT_SECRET=$(cat /run/secrets/jwt-secret)
export DATABASE_PASSWORD=$(cat /run/secrets/db-password)
export AMAP_API_KEY=$(cat /run/secrets/amap-api-key)
export AMAP_SECURITY_KEY=$(cat /run/secrets/amap-security-key)
export REDIS_PASSWORD=$(cat /run/secrets/redis-password)
```

#### 1.3.2 Docker生产配置
**Docker安全配置**:
```dockerfile
# 多阶段构建，减少攻击面
FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:11-jre AS runtime
WORKDIR /app

# 创建非root用户
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# 复制构建产物
COPY --from=build /app/target/*.jar app.jar

# 安全相关配置
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 2. 安全组件配置

### 2.1 Spring Security配置

#### 2.1.1 安全配置类
```java
// SecurityConfig.java 主要配置
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（API服务，使用JWT）
            .csrf().disable()
            
            // 会话管理（无状态）
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            
            // 授权配置
            .authorizeRequests()
                // 公开接口
                .antMatchers("/api/auth/**", "/api/map/config", 
                           "/api/compliance/**", "/api/public/**").permitAll()
                // 用户接口
                .antMatchers("/api/user/**", "/api/map/**", 
                           "/api/search/**", "/api/details/**").hasRole("USER")
                // 管理接口
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他请求需要认证
                .anyRequest().authenticated()
                .and()
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, 
                           UsernamePasswordAuthenticationFilter.class)
            
            // 异常处理
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
            
            // CORS配置
            .cors();
    }
}
```

#### 2.1.2 密码编码器配置
```java
@Bean
public PasswordEncoder passwordEncoder() {
    // BCrypt加密，salt轮数10
    return new BCryptPasswordEncoder(10);
}
```

### 2.2 JWT配置

#### 2.2.1 JWT工具类配置
```java
@Component
public class JwtUtil {
    // 从环境变量读取密钥
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    // 生成令牌（包含IP和User-Agent绑定）
    public String generateToken(String username, String ip, String userAgent) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("ip", ip);
        claims.put("userAgent", userAgent);
        claims.put("created", new Date());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
    
    // 验证令牌绑定
    public boolean validateTokenBinding(String token, String ip, String userAgent) {
        String tokenIp = getClaimFromToken(token, "ip", String.class);
        String tokenUserAgent = getClaimFromToken(token, "userAgent", String.class);
        
        return ip.equals(tokenIp) && userAgent.equals(tokenUserAgent);
    }
}
```

#### 2.2.2 JWT过滤器配置
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 从请求头获取令牌
        String token = getTokenFromRequest(request);
        
        if (token != null) {
            // 2. 检查令牌黑名单
            if (redisUtil.isTokenBlacklisted(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "令牌已失效");
                return;
            }
            
            // 3. 验证令牌有效性
            if (jwtUtil.validateToken(token)) {
                // 4. 验证令牌绑定（IP+User-Agent）
                String ip = getClientIp(request);
                String userAgent = request.getHeader("User-Agent");
                
                if (jwtUtil.validateTokenBinding(token, ip, userAgent)) {
                    // 5. 自动刷新即将过期的令牌
                    if (jwtUtil.isTokenExpiringSoon(token)) {
                        String newToken = jwtUtil.refreshToken(token, ip, userAgent);
                        response.setHeader("X-New-Token", newToken);
                    }
                    
                    // 6. 设置认证信息
                    String username = jwtUtil.getUsernameFromToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 2.3 速率限制配置

#### 2.3.1 Redis速率限制配置
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    // 速率限制配置
    private static final Map<String, RateLimitConfig> rateLimitConfigs = new HashMap<>();
    
    static {
        // 用户接口：10次/分钟
        rateLimitConfigs.put("/api/user/", new RateLimitConfig(10, 60));
        // 管理接口：30次/分钟
        rateLimitConfigs.put("/api/admin/", new RateLimitConfig(30, 60));
        // 公开接口：60次/分钟
        rateLimitConfigs.put("/api/public/", new RateLimitConfig(60, 60));
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String ip = getClientIp(request);
        
        // 查找匹配的速率限制配置
        RateLimitConfig config = findRateLimitConfig(path);
        
        if (config != null) {
            String key = "rate_limit:" + ip + ":" + path;
            Long count = redisUtil.increment(key, config.getWindowSeconds());
            
            // 设置速率限制头部
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.getMaxRequests()));
            response.setHeader("X-RateLimit-Remaining", 
                String.valueOf(Math.max(0, config.getMaxRequests() - count)));
            response.setHeader("X-RateLimit-Reset", 
                String.valueOf(redisUtil.getExpire(key)));
            
            // 检查是否超过限制
            if (count > config.getMaxRequests()) {
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.getWriter().write("请求过于频繁，请稍后再试");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 2.4 XSS防护配置

#### 2.4.1 XSS过滤器配置
```java
@Component
public class XssFilter extends OncePerRequestFilter {
    
    // 敏感词列表
    private List<String> sensitiveWords;
    
    @PostConstruct
    public void init() {
        // 加载敏感词列表
        sensitiveWords = loadSensitiveWords();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // 包装请求，进行XSS过滤
        XssRequestWrapper wrappedRequest = new XssRequestWrapper(request, sensitiveWords);
        
        filterChain.doFilter(wrappedRequest, response);
    }
}

// XSS请求包装器
class XssRequestWrapper extends HttpServletRequestWrapper {
    
    public XssRequestWrapper(HttpServletRequest request, List<String> sensitiveWords) {
        super(request);
        this.sensitiveWords = sensitiveWords;
    }
    
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }
    
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        
        String[] cleanedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanedValues[i] = cleanXss(values[i]);
        }
        return cleanedValues;
    }
    
    private String cleanXss(String value) {
        if (value == null) return null;
        
        // 1. HTML转义
        value = StringEscapeUtils.escapeHtml4(value);
        
        // 2. 敏感词过滤
        for (String word : sensitiveWords) {
            value = value.replaceAll("(?i)" + Pattern.quote(word), "***");
        }
        
        // 3. 移除危险属性
        value = value.replaceAll("(?i)javascript:", "")
                    .replaceAll("(?i)onclick=", "")
                    .replaceAll("(?i)onload=", "")
                    .replaceAll("(?i)onerror=", "");
        
        return value;
    }
}
```

### 2.5 CORS安全配置

#### 2.5.1 CORS配置类
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源（生产环境配置实际域名）
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("https://yanxiaomap.com");
        
        // 允许的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        
        // 允许的请求头
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Accept");
        
        // 允许暴露的响应头
        config.addExposedHeader("X-New-Token");
        config.addExposedHeader("X-RateLimit-Limit");
        config.addExposedHeader("X-RateLimit-Remaining");
        config.addExposedHeader("X-RateLimit-Reset");
        
        // 允许携带凭证（cookies）
        config.setAllowCredentials(true);
        
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
```

## 3. 安全策略配置

### 3.1 security-policy.yml配置

#### 3.1.1 完整配置示例
```yaml
# 安全策略配置
security:
  # 密码策略
  password:
    min-length: 8
    require-uppercase: true
    require-lowercase: true
    require-digits: true
    require-special-chars: true
    history-size: 5
    max-age-days: 90
  
  # 会话安全
  session:
    timeout-minutes: 30
    absolute-timeout-hours: 12
    concurrent-sessions: 3
    cookie:
      http-only: true
      secure: true
      same-site: lax
  
  # SQL注入防护
  sql-injection:
    parameterized-queries: true
    input-validation: true
    min-privilege: true
    monitoring: true
  
  # XSS防护
  xss:
    input-filtering: true
    output-encoding: true
    csp:
      enabled: true
      policy: "default-src 'self'; script-src 'self' 'unsafe-inline' https://webapi.amap.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://webapi.amap.com"
  
  # CSRF防护
  csrf:
    enabled: true
    token-required: true
    same-site-cookie: true
    referrer-validation: true
  
  # 速率限制
  rate-limiting:
    user-endpoints: 10  # 请求/分钟
    admin-endpoints: 30  # 请求/分钟
    public-endpoints: 60 # 请求/分钟
    ip-blacklist-threshold: 100  # 触发IP黑名单的请求次数
  
  # 文件上传安全
  file-upload:
    allowed-types:
      - image/jpeg
      - image/png
      - image/gif
      - application/pdf
      - application/msword
    max-size-mb: 10
    virus-scan: true
    storage-isolation: true
    rename-policy: random
  
  # 敏感数据处理
  sensitive-data:
    encryption:
      passwords: bcrypt
      sensitive-fields: true
      ssl-tls: true
    key-rotation:
      jwt-secret-days: 90
      db-password-days: 180
      api-key-days: 365
  
  # 安全监控
  monitoring:
    realtime-monitoring: true
    attack-detection: true
    compliance-checking: true
    alerting:
      email: true
      sms: true
      im: true
  
  # 合规要求
  compliance:
    icp-filing:
      required: true
      number: "京ICP备XXXXXX号"
    police-filing:
      required: true
      number: "京公网安备XXXXXX号"
    privacy-policy:
      required: true
      versioning: true
      consent-recording: true
    gdpr:
      data-subject-rights: true
      data-protection-officer: true
      impact-assessments: true
```

### 3.2 敏感词列表配置

#### 3.2.1 sensitive-words.txt格式
```
# 敏感词过滤列表
# 格式：每行一个敏感词或短语

# 政治敏感词
[敏感词1]
[敏感词2]

# 违法信息
[敏感词3]
[敏感词4]

# 暴力恐怖
[敏感词5]
[敏感词6]

# 色情低俗
[敏感词7]
[敏感词8]

# 其他敏感内容
[敏感词9]
[敏感词10]
```

#### 3.2.2 敏感词加载配置
```java
@Component
public class SensitiveWordFilter {
    
    private Set<String> sensitiveWords = new HashSet<>();
    
    @PostConstruct
    public void init() {
        try {
            // 从文件加载敏感词
            InputStream inputStream = getClass().getResourceAsStream("/security/sensitive-words.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    sensitiveWords.add(line.toLowerCase());
                }
            }
            
            reader.close();
            logger.info("加载敏感词数量: " + sensitiveWords.size());
        } catch (IOException e) {
            logger.error("加载敏感词文件失败", e);
        }
    }
    
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String lowerText = text.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
```

## 4. 合规配置

### 4.1 备案信息配置

#### 4.1.1 备案信息API配置
```java
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {
    
    @Value("${compliance.icp.number}")
    private String icpNumber;
    
    @Value("${compliance.police.number}")
    private String policeNumber;
    
    @GetMapping("/filing-info")
    public Result<FilingInfo> getFilingInfo() {
        FilingInfo info = new FilingInfo();
        info.setIcpNumber(icpNumber);
        info.setPoliceNumber(policeNumber);
        info.setFilingDate("2026-04-18");
        info.setValidityPeriod("长期有效");
        info.setWebsiteName("研校地图");
        info.setDomain("yanxiaomap.com");
        
        return Result.success(info);
    }
}
```

#### 4.1.2 备案信息环境变量
```properties
# 备案信息配置
compliance.icp.number=京ICP备XXXXXX号
compliance.police.number=京公网安备XXXXXX号
compliance.website.name=研校地图
compliance.domain=yanxiaomap.com
```

### 4.2 隐私政策配置

#### 4.2.1 隐私政策版本管理
```java
@Service
public class PrivacyPolicyService {
    
    // 隐私政策版本信息
    private static final Map<String, PrivacyPolicy> POLICIES = new HashMap<>();
    
    static {
        // 版本1.0 - 初始版本
        POLICIES.put("1.0", PrivacyPolicy.builder()
            .version("1.0")
            .effectiveDate("2026-04-18")
            .content(loadPolicyContent("1.0"))
            .requiredConsent(true)
            .build());
        
        // 版本1.1 - 更新数据处理条款
        POLICIES.put("1.1", PrivacyPolicy.builder()
            .version("1.1")
            .effectiveDate("2026-10-18")
            .content(loadPolicyContent("1.1"))
            .requiredConsent(true)
            .changes("更新了数据处理条款，增加用户权利说明")
            .build());
    }
    
    public PrivacyPolicy getLatestPolicy() {
        return POLICIES.get("1.1");
    }
    
    public PrivacyPolicy getPolicyByVersion(String version) {
        return POLICIES.get(version);
    }
}
```

#### 4.2.2 隐私政策HTML模板
```html
<!-- privacy-policy.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>隐私政策 - 研校地图</title>
    <style>
        /* 隐私政策样式 */
        .policy-container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            font-family: "Microsoft YaHei", sans-serif;
        }
        .policy-header {
            border-bottom: 2px solid #1890ff;
            padding-bottom: 10px;
            margin-bottom: 20px;
        }
        .policy-version {
            color: #666;
            font-size: 14px;
        }
        .policy-section {
            margin-bottom: 25px;
        }
        .policy-section h3 {
            color: #333;
            border-left: 4px solid #1890ff;
            padding-left: 10px;
        }
        .consent-box {
            background-color: #f5f5f5;
            border: 1px solid #ddd;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="policy-container">
        <div class="policy-header">
            <h1>隐私政策</h1>
            <div class="policy-version">
                版本：{{version}} | 生效日期：{{effectiveDate}}
            </div>
        </div>
        
        <div th:utext="${content}"></div>
        
        <div class="consent-box" th:if="${requiredConsent}">
            <h3>用户同意</h3>
            <p>请仔细阅读以上隐私政策内容。点击"同意"表示您已阅读并同意本隐私政策。</p>
            <button onclick="acceptPolicy()">同意隐私政策</button>
            <button onclick="rejectPolicy()">不同意</button>
        </div>
    </div>
    
    <script>
        function acceptPolicy() {
            // 调用API记录用户同意
            fetch('/api/compliance/privacy-policy/consent', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    version: '{{version}}',
                    action: 'accept'
                })
            }).then(() => {
                alert('感谢您的同意！');
                window.location.href = '/';
            });
        }
        
        function rejectPolicy() {
            alert('您需要同意隐私政策才能使用本服务。');
            window.location.href = '/';
        }
    </script>
</body>
</html>
```

## 5. 监控与日志配置

### 5.1 安全日志配置

#### 5.1.1 Logback安全日志配置
```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 安全日志单独文件 -->
    <appender name="SECURITY_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/security.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 安全日志记录器 -->
    <logger name="com.yanxiaomap.security" level="INFO" additivity="false">
        <appender-ref ref="SECURITY_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </logger>
    
    <!-- 认证授权日志 -->
    <logger name="org.springframework.security" level="WARN">
        <appender-ref ref="SECURITY_FILE"/>
    </logger>
</configuration>
```

#### 5.1.2 安全事件日志记录
```java
@Component
public class SecurityLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("SECURITY");
    
    public void logLoginSuccess(String username, String ip) {
        logger.info("登录成功 - 用户: {}, IP: {}, 时间: {}", 
                   username, ip, new Date());
    }
    
    public void logLoginFailure(String username, String ip, String reason) {
        logger.warn("登录失败 - 用户: {}, IP: {}, 原因: {}, 时间: {}", 
                   username, ip, reason, new Date());
    }
    
    public void logAttackAttempt(String attackType, String ip, String payload) {
        logger.error("攻击尝试 - 类型: {}, IP: {}, 载荷: {}, 时间: {}", 
                    attackType, ip, 
                    StringUtils.abbreviate(payload, 100), new Date());
    }
}
```

### 5.2 监控配置

#### 5.2.1 Spring Boot Actuator安全配置
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
    enabled-by-default: false
  endpoint:
    health:
      enabled: true
      show-details: when-authorized
    info:
      enabled: true
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 5.2.2 自定义健康检查
```java
@Component
public class SecurityHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 检查安全组件状态
        boolean jwtHealthy = checkJwtService();
        boolean redisHealthy = checkRedisConnection();
        boolean rateLimitHealthy = checkRateLimitService();
        
        if (jwtHealthy && redisHealthy && rateLimitHealthy) {
            return Health.up()
                .withDetail("jwt_service", "运行正常")
                .withDetail("redis", "连接正常")
                .withDetail("rate_limit", "运行正常")
                .build();
        } else {
            return Health.down()
                .withDetail("jwt_service", jwtHealthy ? "正常" : "异常")
                .withDetail("redis", redisHealthy ? "正常" : "异常")
                .withDetail("rate_limit", rateLimitHealthy ? "正常" : "异常")
                .build();
        }
    }
}
```

## 6. 部署安全检查清单

### 6.1 部署前检查
```bash
#!/bin/bash
# deployment-security-check.sh

echo "=== 部署前安全检查 ==="

# 1. 检查环境变量
echo "1. 检查关键环境变量..."
required_vars=("JWT_SECRET" "DATABASE_PASSWORD" "AMAP_API_KEY")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "错误: 环境变量 $var 未设置"
        exit 1
    fi
    echo "  $var: 已设置"
done

# 2. 检查配置文件
echo "2. 检查安全配置文件..."
config_files=(
    "backend/src/main/resources/security/security-policy.yml"
    "backend/src/main/resources/security/sensitive-words.txt"
    "backend/src/main/resources/security/blacklist-ips.txt"
)

for file in "${config_files[@]}"; do
    if [ ! -f "$file" ]; then
        echo "警告: 配置文件 $file 不存在"
    else
        echo "  $file: 存在"
    fi
done

# 3. 检查端口安全
echo "3. 检查端口配置..."
unsafe_ports=("8080" "3306" "6379")
for port in "${unsafe_ports[@]}"; do
    if netstat -tuln | grep ":$port " > /dev/null; then
        echo " 端口 $port: 已监听"
    fi
done

echo "=== 安全检查完成 ==="
```

## 7. 故障排查指南

### 7.1 常见安全问题排查

#### 问题1: JWT认证失败
**症状**: 401 Unauthorized 错误  
**排查步骤**:
1. 检查JWT令牌是否过期
   ```bash
   # 解码JWT令牌（使用jwt.io或本地工具）
   echo "令牌内容" | base64 -d
   ```
2. 检查令牌绑定（IP/User-Agent）
3. 检查Redis黑名单
   ```bash
   redis-cli GET "token:blacklist:<token_hash>"
   ```

#### 问题2: 速率限制过严
**症状**: 429 Too Many Requests 错误  
**排查步骤**:
1. 检查Redis中的计数
   ```bash
   redis-cli KEYS "rate_limit:*"
   ```
2. 调整速率限制配置
   ```yaml
   # 临时调整配置
   rate-limiting:
     user-endpoints: 20  # 增加限制
   ```

#### 问题3: XSS过滤误报
**症状**: 正常内容被过滤  
**排查步骤**:
1. 检查敏感词列表
   ```bash
   grep "被过滤内容" backend/src/main/resources/security/sensitive-words.txt
   ```
2. 检查XSS过滤规则
3. 添加白名单规则

## 8. 附录

### 8.1 配置文件路径汇总
| 配置文件 | 路径 | 用途 |
|----------|------|------|
| 安全策略配置 | `backend/src/main/resources/security/security-policy.yml` | 主安全配置 |
| 敏感词列表 | `backend/src/main/resources/security/sensitive-words.txt` | 内容过滤 |
| IP黑名单 | `backend/src/main/resources/security/blacklist-ips.txt` | IP封禁 |
| 应用配置 | `backend/src/main/resources/application.yml` | Spring配置 |
| 日志配置 | `backend/src/main/resources/logback-spring.xml` | 日志配置 |

### 8.2 环境变量汇总
| 变量名 | 用途 | 示例值 |
|--------|------|--------|
| `JWT_SECRET` | JWT签名密钥 | `your-secret-key-change-in-production` |
| `JWT_EXPIRATION` | JWT有效期（秒） | `7200` |
| `BCRYPT_SALT_ROUNDS` | BCrypt salt轮数 | `10` |
| `AMAP_API_KEY` | 高德地图API密钥 | `f2a14d5a5748760eea937b4a756d6e81` |
| `DATABASE_PASSWORD` | 数据库密码 | `SecureDbPass123!` |
| `REDIS_PASSWORD` | Redis密码 | `RedisPass456!` |

### 8.3 安全API端点
| 端点 | 方法 | 用途 | 认证要求 |
|------|------|------|----------|
| `/api/auth/login` | POST | 用户登录 | 无 |
| `/api/auth/logout` | POST | 用户注销 | JWT |
| `/api/auth/refresh` | POST | 刷新令牌 | JWT |
| `/api/compliance/filing-info` | GET | 获取备案信息 | 无 |
| `/api/compliance/privacy-policy` | GET | 获取隐私政策 | 无 |
| `/api/admin/security/logs` | GET | 查看安全日志 | ADMIN |
| `/api/admin/security/blacklist` | GET/POST | IP黑名单管理 | ADMIN |

---

**文档版本**: 1.0  
**最后更新**: 2026-04-18  
**适用环境**: 开发、测试、生产  
**负责人**: 安全配置团队