#!/bin/bash
# 漏洞扫描脚本
# 功能：扫描系统安全漏洞，生成漏洞报告
# 支持平台：Linux/macOS (Windows用户请使用对应的PowerShell脚本)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 检查依赖工具
check_dependencies() {
    log_info "检查依赖工具..."
    
    local missing_tools=()
    
    # 检查curl
    if ! command -v curl &> /dev/null; then
        missing_tools+=("curl")
    fi
    
    # 检查nmap
    if ! command -v nmap &> /dev/null; then
        missing_tools+=("nmap")
    fi
    
    # 检查nikto
    if ! command -v nikto &> /dev/null; then
        missing_tools+=("nikto")
    fi
    
    # 检查sqlmap
    if ! command -v sqlmap &> /dev/null; then
        missing_tools+=("sqlmap")
    fi
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_warning "缺少以下工具: ${missing_tools[*]}"
        log_info "建议安装命令:"
        for tool in "${missing_tools[@]}"; do
            case $tool in
                "curl")
                    echo "  Ubuntu/Debian: sudo apt install curl"
                    echo "  CentOS/RHEL: sudo yum install curl"
                    ;;
                "nmap")
                    echo "  Ubuntu/Debian: sudo apt install nmap"
                    echo "  CentOS/RHEL: sudo yum install nmap"
                    ;;
                "nikto")
                    echo "  Ubuntu/Debian: sudo apt install nikto"
                    echo "  CentOS/RHEL: sudo yum install nikto"
                    ;;
                "sqlmap")
                    echo "  下载: https://github.com/sqlmapproject/sqlmap"
                    ;;
            esac
        done
        return 1
    fi
    
    log_success "所有依赖工具已安装"
    return 0
}

# 扫描端口和服务
scan_ports() {
    local target=${1:-"localhost"}
    local output_file=${2:-"scan_results/ports_scan.txt"}
    
    log_info "扫描端口和服务: $target"
    
    mkdir -p scan_results
    
    # 快速扫描常见端口
    nmap -T4 -F "$target" > "$output_file"
    
    if [ $? -eq 0 ]; then
        log_success "端口扫描完成，结果保存到: $output_file"
        
        # 分析开放端口
        local open_ports=$(grep -E '^[0-9]+/.*open' "$output_file" | wc -l)
        log_info "发现 $open_ports 个开放端口"
        
        # 检查高风险端口
        check_high_risk_ports "$output_file"
    else
        log_error "端口扫描失败"
    fi
}

# 检查高风险端口
check_high_risk_ports() {
    local scan_file=$1
    
    log_info "检查高风险端口..."
    
    # 高风险端口列表
    declare -A high_risk_ports=(
        ["21"]="FTP (可能弱密码)"
        ["22"]="SSH (可能暴力破解)"
        ["23"]="Telnet (明文传输)"
        ["25"]="SMTP (可能开放中继)"
        ["80"]="HTTP (可能Web漏洞)"
        ["443"]="HTTPS (可能配置问题)"
        ["3306"]="MySQL (可能弱密码)"
        ["3389"]="RDP (可能暴力破解)"
        ["8080"]="HTTP代理 (可能未授权访问)"
        ["9200"]="Elasticsearch (可能未授权访问)"
    )
    
    local found_high_risk=0
    
    for port in "${!high_risk_ports[@]}"; do
        if grep -q "^$port/.*open" "$scan_file"; then
            log_warning "高风险端口 $port 开放: ${high_risk_ports[$port]}"
            found_high_risk=1
        fi
    done
    
    if [ $found_high_risk -eq 0 ]; then
        log_success "未发现高风险端口"
    fi
}

# Web应用漏洞扫描
scan_web_vulnerabilities() {
    local url=${1:-"http://localhost:8080"}
    local output_file=${2:-"scan_results/web_vuln_scan.txt"}
    
    log_info "扫描Web应用漏洞: $url"
    
    mkdir -p scan_results
    
    # 使用nikto进行Web漏洞扫描
    nikto -h "$url" -output "$output_file" -Format txt
    
    if [ $? -eq 0 ]; then
        log_success "Web漏洞扫描完成，结果保存到: $output_file"
        
        # 统计发现的问题
        local issues=$(grep -c "+" "$output_file" 2>/dev/null || echo "0")
        log_info "发现 $issues 个潜在问题"
    else
        log_error "Web漏洞扫描失败"
    fi
}

# SQL注入扫描
scan_sql_injection() {
    local url=${1:-"http://localhost:8080"}
    local output_dir=${2:-"scan_results/sqlmap"}
    
    log_info "扫描SQL注入漏洞: $url"
    
    mkdir -p "$output_dir"
    
    # 使用sqlmap进行SQL注入扫描（仅基本扫描，避免过度请求）
    sqlmap -u "$url" --batch --level=1 --risk=1 --output-dir="$output_dir"
    
    if [ $? -eq 0 ]; then
        log_success "SQL注入扫描完成，结果保存到: $output_dir"
    else
        log_error "SQL注入扫描失败"
    fi
}

# 检查SSL/TLS配置
check_ssl_tls() {
    local host=${1:-"localhost"}
    local port=${2:-"443"}
    local output_file=${3:-"scan_results/ssl_check.txt"}
    
    log_info "检查SSL/TLS配置: $host:$port"
    
    mkdir -p scan_results
    
    # 使用openssl检查SSL证书
    echo "=== SSL证书信息 ===" > "$output_file"
    openssl s_client -connect "$host:$port" -servername "$host" < /dev/null 2>/dev/null | \
        openssl x509 -noout -text >> "$output_file" 2>&1
    
    echo "" >> "$output_file"
    echo "=== SSL/TLS协议支持 ===" >> "$output_file"
    
    # 检查支持的协议
    local protocols=("ssl2" "ssl3" "tls1" "tls1_1" "tls1_2" "tls1_3")
    
    for protocol in "${protocols[@]}"; do
        if openssl s_client -connect "$host:$port" -"$protocol" < /dev/null 2>/dev/null | grep -q "CONNECTED"; then
            echo "支持 $protocol" >> "$output_file"
        else
            echo "不支持 $protocol" >> "$output_file"
        fi
    done
    
    log_success "SSL/TLS检查完成，结果保存到: $output_file"
    
    # 检查弱加密算法
    check_weak_ciphers "$host" "$port" "$output_file"
}

# 检查弱加密算法
check_weak_ciphers() {
    local host=$1
    local port=$2
    local output_file=$3
    
    log_info "检查弱加密算法..."
    
    echo "" >> "$output_file"
    echo "=== 弱加密算法检查 ===" >> "$output_file"
    
    # 弱加密算法列表
    local weak_ciphers=(
        "NULL"
        "EXPORT"
        "RC4"
        "DES"
        "3DES"
        "MD5"
        "SHA1"
        "CBC"
    )
    
    local found_weak=0
    
    for cipher in "${weak_ciphers[@]}"; do
        if openssl s_client -connect "$host:$port" -cipher "$cipher" < /dev/null 2>/dev/null | grep -q "Cipher is"; then
            echo "发现弱加密算法: $cipher" >> "$output_file"
            found_weak=1
        fi
    done
    
    if [ $found_weak -eq 0 ]; then
        echo "未发现弱加密算法" >> "$output_file"
        log_success "未发现弱加密算法"
    else
        log_warning "发现弱加密算法，详情见报告"
    fi
}

# 检查文件权限
check_file_permissions() {
    local target_dir=${1:-"."}
    local output_file=${2:-"scan_results/file_permissions.txt"}
    
    log_info "检查文件权限: $target_dir"
    
    mkdir -p scan_results
    
    echo "=== 文件权限检查 ===" > "$output_file"
    echo "检查时间: $(date)" >> "$output_file"
    echo "" >> "$output_file"
    
    # 检查世界可写文件
    echo "世界可写文件:" >> "$output_file"
    find "$target_dir" -type f -perm -o+w -ls 2>/dev/null >> "$output_file"
    
    echo "" >> "$output_file"
    
    # 检查世界可读的敏感文件
    echo "世界可读的敏感文件:" >> "$output_file"
    find "$target_dir" -type f \( -name "*.key" -o -name "*.pem" -o -name "*.crt" -o -name "*.conf" -o -name "*.yml" -o -name "*.yaml" -o -name "*.properties" \) -perm -o+r -ls 2>/dev/null >> "$output_file"
    
    local world_writable=$(find "$target_dir" -type f -perm -o+w 2>/dev/null | wc -l)
    local sensitive_readable=$(find "$target_dir" -type f \( -name "*.key" -o -name "*.pem" -o -name "*.crt" \) -perm -o+r 2>/dev/null | wc -l)
    
    if [ "$world_writable" -gt 0 ]; then
        log_warning "发现 $world_writable 个世界可写文件"
    fi
    
    if [ "$sensitive_readable" -gt 0 ]; then
        log_warning "发现 $sensitive_readable 个世界可读的敏感文件"
    fi
    
    if [ "$world_writable" -eq 0 ] && [ "$sensitive_readable" -eq 0 ]; then
        log_success "文件权限检查正常"
    fi
    
    log_success "文件权限检查完成，结果保存到: $output_file"
}

# 生成总结报告
generate_summary_report() {
    local output_file=${1:-"scan_results/security_summary_$(date +%Y%m%d_%H%M%S).md"}
    
    log_info "生成安全总结报告..."
    
    mkdir -p scan_results
    
    cat > "$output_file" << EOF
# 安全扫描总结报告
生成时间: $(date)

## 扫描概况
- 扫描开始时间: $SCAN_START_TIME
- 扫描结束时间: $(date)
- 扫描目标: $TARGET

## 发现的问题

### 1. 端口扫描结果
\`\`\`
$(cat scan_results/ports_scan.txt 2>/dev/null || echo "无扫描结果")
\`\`\`

### 2. Web漏洞扫描结果
\`\`\`
$(head -50 scan_results/web_vuln_scan.txt 2>/dev/null || echo "无扫描结果")
\`\`\`

### 3. SSL/TLS配置检查
\`\`\`
$(head -30 scan_results/ssl_check.txt 2>/dev/null || echo "无扫描结果")
\`\`\`

### 4. 文件权限检查
\`\`\`
$(head -30 scan_results/file_permissions.txt 2>/dev/null || echo "无扫描结果")
\`\`\`

## 建议措施

1. **端口安全**
   - 关闭不必要的端口
   - 配置防火墙规则
   - 使用强密码认证

2. **Web安全**
   - 定期更新Web应用
   - 配置WAF防护
   - 启用HTTPS

3. **SSL/TLS安全**
   - 禁用弱加密算法
   - 启用HSTS
   - 定期更新证书

4. **文件安全**
   - 设置合适的文件权限
   - 保护敏感配置文件
   - 定期审计权限

## 风险等级评估
- 高风险问题: $(grep -c "高风险" scan_results/*.txt 2>/dev/null || echo "0")
- 中风险问题: $(grep -c "中风险" scan_results/*.txt 2>/dev/null || echo "0")
- 低风险问题: $(grep -c "低风险" scan_results/*.txt 2>/dev/null || echo "0")

## 下一步建议
1. 立即修复高风险问题
2. 一周内修复中风险问题
3. 一个月内修复低风险问题
4. 建立定期安全扫描机制

---
*报告生成系统: 燕小图安全扫描工具*
*版本: 1.0*
EOF

    log_success "总结报告生成完成: $output_file"
}

# 主函数
main() {
    echo "========================================="
    echo "      燕小图安全漏洞扫描工具 v1.0        "
    echo "========================================="
    
    # 记录开始时间
    SCAN_START_TIME=$(date)
    
    # 解析参数
    TARGET="localhost"
    SCAN_TYPE="all"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--target)
                TARGET="$2"
                shift 2
                ;;
            -s|--scan)
                SCAN_TYPE="$2"
                shift 2
                ;;
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
    
    log_info "开始安全扫描，目标: $TARGET"
    
    # 检查依赖
    if ! check_dependencies; then
        log_warning "部分依赖工具缺失，扫描可能不完整"
        read -p "是否继续? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # 根据扫描类型执行扫描
    case $SCAN_TYPE in
        "ports")
            scan_ports "$TARGET"
            ;;
        "web")
            scan_web_vulnerabilities "http://$TARGET:8080"
            ;;
        "ssl")
            check_ssl_tls "$TARGET"
            ;;
        "files")
            check_file_permissions "."
            ;;
        "all")
            scan_ports "$TARGET"
            scan_web_vulnerabilities "http://$TARGET:8080"
            check_ssl_tls "$TARGET"
            check_file_permissions "."
            scan_sql_injection "http://$TARGET:8080"
            ;;
        *)
            log_error "未知的扫描类型: $SCAN_TYPE"
            show_help
            exit 1
            ;;
    esac
    
    # 生成总结报告
    generate_summary_report
    
    echo "========================================="
    log_success "安全扫描完成!"
    echo "详细报告请查看 scan_results/ 目录"
    echo "========================================="
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
  -t, --target <主机>     扫描目标主机 (默认: localhost)
  -s, --scan <类型>       扫描类型:
                          ports     - 端口扫描
                          web       - Web漏洞扫描
                          ssl       - SSL/TLS检查
                          files     - 文件权限检查
                          all       - 全面扫描 (默认)
  -h, --help             显示此帮助信息

示例:
  $0 -t example.com -s ports      # 扫描example.com的端口
  $0 -t 192.168.1.1 -s all        # 全面扫描192.168.1.1
  $0                              # 全面扫描localhost

注意:
  1. 需要管理员权限运行部分扫描
  2. 扫描可能对目标系统造成负载
  3. 仅用于授权测试
EOF
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi