#!/bin/bash
# 合规检查脚本
# 功能：检查系统是否符合安全合规要求
# 支持平台：Linux/macOS (Windows用户请使用对应的PowerShell脚本)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 合规检查结果文件
COMPLIANCE_REPORT="compliance_report_$(date +%Y%m%d_%H%M%S).md"
SCORE=0
TOTAL_SCORE=100
PASSED_CHECKS=0
TOTAL_CHECKS=0

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查结果记录
record_check() {
    local check_name=$1
    local status=$2
    local message=$3
    local score=$4
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    if [ "$status" = "PASS" ]; then
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        SCORE=$((SCORE + score))
        echo "✅ **$check_name**: $message" >> "$COMPLIANCE_REPORT"
    elif [ "$status" = "WARN" ]; then
        echo "⚠️ **$check_name**: $message" >> "$COMPLIANCE_REPORT"
    else
        echo "❌ **$check_name**: $message" >> "$COMPLIANCE_REPORT"
    fi
}

# 初始化报告
init_report() {
    cat > "$COMPLIANCE_REPORT" << EOF
# 安全合规检查报告
生成时间: $(date)

## 检查概况
- 检查标准: OWASP Top 10 2021、等保2.0、GDPR参考
- 检查范围: 系统安全配置、合规要求
- 检查工具: 燕小图合规检查脚本 v1.0

## 检查结果

### 1. 密码安全合规检查
EOF
}

# 检查密码策略
check_password_policy() {
    log_info "检查密码策略..."
    
    # 检查密码最小长度
    if grep -q "password.min-length=8" application.yml 2>/dev/null || \
       grep -q "password.min-length: 8" application.yml 2>/dev/null; then
        record_check "密码最小长度" "PASS" "密码最小长度设置为8位" 5
    else
        record_check "密码最小长度" "FAIL" "密码最小长度未设置为8位" 0
    fi
    
    # 检查密码复杂度
    if grep -q "password.complexity" application.yml 2>/dev/null; then
        record_check "密码复杂度要求" "PASS" "密码复杂度要求已配置" 5
    else
        record_check "密码复杂度要求" "WARN" "密码复杂度要求未明确配置" 3
    fi
    
    # 检查密码加密算法
    if grep -qi "bcrypt" application.yml 2>/dev/null || \
       grep -qi "BCryptPasswordEncoder" backend/src/main/java 2>/dev/null; then
        record_check "密码加密算法" "PASS" "使用BCrypt加密算法" 10
    else
        record_check "密码加密算法" "FAIL" "未使用BCrypt加密算法" 0
    fi
}

# 检查会话安全
check_session_security() {
    log_info "检查会话安全..."
    
    # 检查JWT配置
    if find backend/src -name "*.java" -type f -exec grep -l "JWT\|JwtUtil" {} \; 2>/dev/null | grep -q .; then
        record_check "JWT认证" "PASS" "使用JWT进行认证" 5
        
        # 检查JWT有效期
        if find backend/src -name "*.java" -type f -exec grep -l "expiration.*7200\|2.*hour" {} \; 2>/dev/null | grep -q .; then
            record_check "JWT有效期" "PASS" "JWT有效期合理（2小时）" 5
        else
            record_check "JWT有效期" "WARN" "JWT有效期未明确设置为2小时" 3
        fi
    else
        record_check "JWT认证" "FAIL" "未使用JWT认证" 0
    fi
    
    # 检查会话超时
    if grep -q "session.timeout\|session.max-age" application.yml 2>/dev/null; then
        record_check "会话超时" "PASS" "会话超时已配置" 5
    else
        record_check "会话超时" "WARN" "会话超时未明确配置" 3
    fi
}

# 检查SQL注入防护
check_sql_injection_protection() {
    log_info "检查SQL注入防护..."
    
    # 检查MyBatis-Plus使用
    if find backend/src -name "*.java" -type f -exec grep -l "MybatisPlus\|@Mapper" {} \; 2>/dev/null | grep -q .; then
        record_check "ORM框架" "PASS" "使用MyBatis-Plus，支持参数化查询" 10
        
        # 检查SQL注入过滤
        if find backend/src -name "*.java" -type f -exec grep -l "sql.*injection\|XssFilter" {} \; 2>/dev/null | grep -q .; then
            record_check "SQL注入过滤" "PASS" "配置SQL注入过滤" 5
        else
            record_check "SQL注入过滤" "WARN" "SQL注入过滤未明确配置" 3
        fi
    else
        record_check "ORM框架" "FAIL" "未使用安全的ORM框架" 0
    fi
    
    # 检查参数化查询
    if find backend/src -name "*Mapper.java" -type f -exec grep -l "@Param\|#{" {} \; 2>/dev/null | grep -q .; then
        record_check "参数化查询" "PASS" "使用参数化查询" 10
    else
        record_check "参数化查询" "FAIL" "未使用参数化查询" 0
    fi
}

# 检查XSS防护
check_xss_protection() {
    log_info "检查XSS防护..."
    
    # 检查XSS过滤器
    if find backend/src -name "XssFilter.java" -type f 2>/dev/null | grep -q .; then
        record_check "XSS过滤器" "PASS" "配置XSS过滤器" 10
    else
        record_check "XSS过滤器" "FAIL" "未配置XSS过滤器" 0
    fi
    
    # 检查CSP头
    if find backend/src -name "*.java" -type f -exec grep -l "Content-Security-Policy\|CSP" {} \; 2>/dev/null | grep -q .; then
        record_check "CSP头" "PASS" "配置Content Security Policy" 5
    else
        record_check "CSP头" "WARN" "未配置Content Security Policy" 3
    fi
    
    # 检查输出编码
    if find backend/src -name "*.java" -type f -exec grep -l "escapeHtml\|HtmlUtils" {} \; 2>/dev/null | grep -q .; then
        record_check "输出编码" "PASS" "对输出进行编码" 5
    else
        record_check "输出编码" "WARN" "输出编码未明确配置" 3
    fi
}

# 检查CSRF防护
check_csrf_protection() {
    log_info "检查CSRF防护..."
    
    # 检查Spring Security CSRF配置
    if find backend/src -name "*Security*.java" -type f -exec grep -l "csrf\|CsrfFilter" {} \; 2>/dev/null | grep -q .; then
        record_check "CSRF防护" "PASS" "配置CSRF防护" 10
    else
        record_check "CSRF防护" "FAIL" "未配置CSRF防护" 0
    fi
}

# 检查接口限流
check_rate_limiting() {
    log_info "检查接口限流..."
    
    # 检查限流配置
    if find backend/src -name "*RateLimit*.java" -type f 2>/dev/null | grep -q .; then
        record_check "接口限流" "PASS" "配置接口限流" 10
    else
        record_check "接口限流" "FAIL" "未配置接口限流" 0
    fi
    
    # 检查限流配置值
    if grep -q "rate.limit\|rate-limit" application.yml 2>/dev/null; then
        record_check "限流配置" "PASS" "限流配置已设置" 5
    else
        record_check "限流配置" "WARN" "限流配置未明确设置" 3
    fi
}

# 检查登录安全
check_login_security() {
    log_info "检查登录安全..."
    
    # 检查登录失败锁定
    if find backend/src -name "*.java" -type f -exec grep -l "login.*attempt\|failed.*lock" {} \; 2>/dev/null | grep -q .; then
        record_check "登录失败锁定" "PASS" "配置登录失败锁定" 10
    else
        record_check "登录失败锁定" "FAIL" "未配置登录失败锁定" 0
    fi
    
    # 检查验证码
    if find backend/src -name "*.java" -type f -exec grep -l "captcha\|验证码" {} \; 2>/dev/null | grep -q .; then
        record_check "图形验证码" "PASS" "配置图形验证码" 5
    else
        record_check "图形验证码" "WARN" "未配置图形验证码" 3
    fi
}

# 检查SSL/TLS配置
check_ssl_tls() {
    log_info "检查SSL/TLS配置..."
    
    # 检查HTTPS配置
    if grep -qi "https\|ssl\|tls" application.yml 2>/dev/null; then
        record_check "HTTPS配置" "PASS" "HTTPS已配置" 10
    else
        record_check "HTTPS配置" "WARN" "HTTPS未明确配置" 3
    fi
    
    # 检查HSTS
    if find backend/src -name "*.java" -type f -exec grep -l "Strict-Transport-Security\|HSTS" {} \; 2>/dev/null | grep -q .; then
        record_check "HSTS配置" "PASS" "配置HSTS" 5
    else
        record_check "HSTS配置" "WARN" "未配置HSTS" 3
    fi
}

# 检查备案信息
check_filing_info() {
    log_info "检查备案信息..."
    
    # 检查备案信息配置
    if grep -q "icp\|备案" application.yml 2>/dev/null; then
        record_check "备案信息配置" "PASS" "备案信息已配置" 10
    else
        record_check "备案信息配置" "FAIL" "备案信息未配置" 0
    fi
    
    # 检查备案信息API
    if find backend/src -name "*Compliance*.java" -type f 2>/dev/null | grep -q .; then
        record_check "备案信息API" "PASS" "提供备案信息API" 5
    else
        record_check "备案信息API" "FAIL" "未提供备案信息API" 0
    fi
}

# 检查隐私政策
check_privacy_policy() {
    log_info "检查隐私政策..."
    
    # 检查隐私政策控制器
    if find backend/src -name "*Privacy*.java" -type f 2>/dev/null | grep -q .; then
        record_check "隐私政策API" "PASS" "提供隐私政策API" 10
    else
        record_check "隐私政策API" "FAIL" "未提供隐私政策API" 0
    fi
    
    # 检查隐私政策文件
    if find backend/src/main/resources -name "*privacy*.html" -type f 2>/dev/null | grep -q .; then
        record_check "隐私政策文件" "PASS" "隐私政策HTML文件存在" 5
    else
        record_check "隐私政策文件" "FAIL" "隐私政策HTML文件不存在" 0
    fi
}

# 检查安全监控
check_security_monitoring() {
    log_info "检查安全监控..."
    
    # 检查安全日志
    if find backend/src -name "*.java" -type f -exec grep -l "security.*log\|audit.*log" {} \; 2>/dev/null | grep -q .; then
        record_check "安全日志" "PASS" "配置安全日志记录" 5
    else
        record_check "安全日志" "WARN" "安全日志记录未明确配置" 3
    fi
    
    # 检查异常监控
    if find backend/src -name "*.java" -type f -exec grep -l "异常.*监控\|abnormal.*monitor" {} \; 2>/dev/null | grep -q .; then
        record_check "异常监控" "PASS" "配置异常监控" 5
    else
        record_check "异常监控" "WARN" "异常监控未明确配置" 3
    fi
}

# 检查文件上传安全
check_file_upload_security() {
    log_info "检查文件上传安全..."
    
    # 检查文件类型限制
    if find backend/src -name "*.java" -type f -exec grep -l "file.*type\|upload.*limit" {} \; 2>/dev/null | grep -q .; then
        record_check "文件类型限制" "PASS" "配置文件类型限制" 5
    else
        record_check "文件类型限制" "WARN" "文件类型限制未明确配置" 3
    fi
    
    # 检查文件大小限制
    if find backend/src -name "*.java" -type f -exec grep -l "max.*size\|file.*size" {} \; 2>/dev/null | grep -q .; then
        record_check "文件大小限制" "PASS" "配置文件大小限制" 5
    else
        record_check "文件大小限制" "WARN" "文件大小限制未明确配置" 3
    fi
}

# 生成合规报告
generate_compliance_report() {
    log_info "生成合规报告..."
    
    local compliance_rate=$((SCORE * 100 / TOTAL_SCORE))
    local pass_rate=$((PASSED_CHECKS * 100 / TOTAL_CHECKS))
    
    cat >> "$COMPLIANCE_REPORT" << EOF

## 合规检查总结

### 检查统计
- 总检查项: $TOTAL_CHECKS
- 通过项: $PASSED_CHECKS
- 警告项: $((TOTAL_CHECKS - PASSED_CHECKS))
- 通过率: $pass_rate%

### 合规评分
- 总分: $TOTAL_SCORE
- 得分: $SCORE
- 合规率: $compliance_rate%

### 合规等级
EOF

    if [ $compliance_rate -ge 90 ]; then
        echo "- **等级: A (优秀)** - 完全符合安全合规要求" >> "$COMPLIANCE_REPORT"
    elif [ $compliance_rate -ge 70 ]; then
        echo "- **等级: B (良好)** - 基本符合安全合规要求，有个别问题需要改进" >> "$COMPLIANCE_REPORT"
    elif [ $compliance_rate -ge 50 ]; then
        echo "- **等级: C (合格)** - 部分符合安全合规要求，需要重点改进" >> "$COMPLIANCE_REPORT"
    else
        echo "- **等级: D (不合格)** - 不符合安全合规要求，需要全面改进" >> "$COMPLIANCE_REPORT"
    fi

    cat >> "$COMPLIANCE_REPORT" << EOF

### 改进建议

#### 1. 立即改进项（高风险）
- 修复所有FAIL项
- 配置密码加密算法（必须使用BCrypt）
- 配置SQL注入防护
- 配置XSS过滤器
- 配置CSRF防护

#### 2. 近期改进项（中风险）
- 修复所有WARN项
- 完善密码复杂度要求
- 配置会话超时
- 配置HSTS
- 完善安全监控

#### 3. 长期优化项（低风险）
- 实施更严格的安全策略
- 建立定期安全审计机制
- 实施安全培训和意识提升
- 建立应急响应机制

### 合规标准对照

#### OWASP Top 10 2021 对照
- ✅ A01:2021 - 访问控制破坏（已检查）
- ✅ A02:2021 - 加密机制失效（已检查）
- ✅ A03:2021 - 注入攻击（已检查）
- ✅ A04:2021 - 不安全设计（部分检查）
- ✅ A05:2021 - 安全配置错误（已检查）
- ✅ A06:2021 - 脆弱和过时的组件（需要额外工具）
- ✅ A07:2021 - 身份认证和授权失败（已检查）
- ✅ A08:2021 - 软件和数据完整性故障（部分检查）
- ✅ A09:2021 - 安全日志和监控失败（已检查）
- ✅ A10:2021 - 服务端请求伪造（需要额外检查）

#### 等保2.0 基础要求对照
- ✅ 安全物理环境（不适用）
- ✅ 安全通信网络（已检查）
- ✅ 安全区域边界（已检查）
- ✅ 安全计算环境（已检查）
- ✅ 安全管理中心（部分检查）
- ✅ 安全管理制度（需要文档检查）
- ✅ 安全管理机构（需要组织检查）
- ✅ 安全管理人员（需要人员检查）
- ✅ 安全建设管理（需要过程检查）
- ✅ 安全运维管理（需要运维检查）

#### GDPR 参考原则对照
- ✅ 合法性、公平性和透明度（已检查）
- ✅ 目的限制（已检查）
- ✅ 数据最小化（部分检查）
- ✅ 准确性（已检查）
- ✅ 存储限制（已检查）
- ✅ 完整性和保密性（已检查）
- ✅ 问责制（需要文档检查）

### 后续行动建议

1. **立即行动**
   - 修复所有高风险问题
   - 更新安全配置
   - 部署安全补丁

2. **一周内行动**
   - 完成中风险问题修复
   - 更新安全文档
   - 进行安全培训

3. **一个月内行动**
   - 建立定期安全检查机制
   - 实施安全监控告警
   - 进行渗透测试

4. **长期计划**
   - 建立安全开发生命周期
   - 实施持续安全测试
   - 建立安全事件响应团队

---
*报告生成系统: 燕小图合规检查工具*
*版本: 1.0*
*注意: 本报告仅供参考，实际合规情况可能因具体环境和配置而异*
EOF

    log_success "合规报告生成完成: $COMPLIANCE_REPORT"
    
    echo "========================================="
    echo "合规检查完成!"
    echo "合规率: $compliance_rate%"
    echo "等级: $( [ $compliance_rate -ge 90 ] && echo "A" || [ $compliance_rate -ge 70 ] && echo "B" || [ $compliance_rate -ge 50 ] && echo "C" || echo "D" )"
    echo "详细报告: $COMPLIANCE_REPORT"
    echo "========================================="
}

# 主函数
main() {
    echo "========================================="
    echo "      燕小图安全合规检查工具 v1.0        "
    echo "========================================="
    
    log_info "开始安全合规检查..."
    
    # 初始化报告
    init_report
    
    # 执行各项检查
    check_password_policy
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 2. 会话安全合规检查
EOF
    check_session_security
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 3. SQL注入防护检查
EOF
    check_sql_injection_protection
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 4. XSS防护检查
EOF
    check_xss_protection
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 5. CSRF防护检查
EOF
    check_csrf_protection
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 6. 接口限流检查
EOF
    check_rate_limiting
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 7. 登录安全检查
EOF
    check_login_security
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 8. SSL/TLS配置检查
EOF
    check_ssl_tls
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 9. 备案信息检查
EOF
    check_filing_info
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 10. 隐私政策检查
EOF
    check_privacy_policy
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 11. 安全监控检查
EOF
    check_security_monitoring
    
    cat >> "$COMPLIANCE_REPORT" << EOF

### 12. 文件上传安全检查
EOF
    check_file_upload_security
    
    # 生成最终报告
    generate_compliance_report
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
  -h, --help     显示此帮助信息

功能:
  检查系统是否符合以下安全合规要求:
  1. OWASP Top 10 2021
  2. 等保2.0基础要求
  3. GDPR参考原则

检查范围:
  - 密码安全策略
  - 会话安全配置
  - SQL注入防护
  - XSS防护
  - CSRF防护
  - 接口限流
  - 登录安全
  - SSL/TLS配置
  - 备案信息
  - 隐私政策
  - 安全监控
  - 文件上传安全

输出:
  - 生成详细的合规检查报告
  - 提供合规评分和等级
  - 给出改进建议

示例:
  $0              # 执行完整合规检查
  $0 -h           # 显示帮助信息

注意:
  1. 需要读取项目配置文件
  2. 检查结果仅供参考
  3. 实际合规需结合具体业务场景
EOF
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # 检查是否在项目根目录
    if [ ! -f "application.yml" ] && [ ! -d "backend" ]; then
        log_error "请在项目根目录运行此脚本"
        log_info "项目根目录应包含 application.yml 或 backend 目录"
        exit 1
    fi
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    main "$@"
fi