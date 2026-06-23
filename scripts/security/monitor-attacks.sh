#!/bin/bash
# 攻击监控脚本
# 功能：监控系统安全事件，检测攻击行为
# 支持平台：Linux/macOS (Windows用户请使用对应的PowerShell脚本)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置文件
CONFIG_FILE="security_monitor.conf"
LOG_DIR="security_logs"
ALERT_THRESHOLD=10  # 报警阈值
CHECK_INTERVAL=60   # 检查间隔（秒）

# 攻击类型定义
declare -A ATTACK_PATTERNS=(
    ["SQL_INJECTION"]="union.*select|select.*from|insert.*into|update.*set|delete.*from|or.*1=1|' OR '1'='1"
    ["XSS_ATTACK"]="<script|javascript:|onclick=|onload=|onerror=|alert\(|eval\(|document\.cookie"
    ["BRUTE_FORCE"]="Failed.*password|Authentication.*failed|Invalid.*credentials|Login.*failed"
    ["CSRF_ATTACK"]="CSRF.*token.*missing|CSRF.*token.*invalid|Cross.*Site.*Request.*Forgery"
    ["PATH_TRAVERSAL"]="\.\./|\.\.\\|/etc/passwd|/etc/shadow|\.\.%2f|\.\.%5c"
    ["COMMAND_INJECTION"]=";.*ls|;.*cat|;.*rm|;.*wget|;.*curl|\\|.*ls|\\|.*cat"
    ["FILE_INCLUSION"]="include.*\.\.|require.*\.\.|include_once.*\.\.|require_once.*\.\.|php://filter"
    ["DDOS_ATTACK"]="Too.*many.*requests|Rate.*limit.*exceeded|Connection.*flood"
    ["SCANNING"]="nmap|nikto|sqlmap|nessus|metasploit|dirb|gobuster"
    ["UNAUTHORIZED_ACCESS"]="Unauthorized|Forbidden|Access.*denied|Permission.*denied"
)

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_alert() {
    echo -e "${RED}🚨 [ALERT]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
    send_alert "$1"
}

# 发送报警
send_alert() {
    local message=$1
    
    # 记录到报警日志
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ALERT: $message" >> "$LOG_DIR/alert.log"
    
    # 这里可以集成邮件、短信、Webhook等报警方式
    # 示例：发送邮件
    # echo "$message" | mail -s "安全报警" admin@example.com
    
    # 示例：发送Webhook
    # curl -X POST -H "Content-Type: application/json" \
    #      -d "{\"text\":\"$message\"}" \
    #      https://hooks.slack.com/services/xxx/xxx/xxx
    
    log_info "报警已发送: $message"
}

# 初始化监控
init_monitor() {
    log_info "初始化安全监控..."
    
    # 创建日志目录
    mkdir -p "$LOG_DIR"
    
    # 创建配置文件（如果不存在）
    if [ ! -f "$CONFIG_FILE" ]; then
        create_config_file
    fi
    
    # 加载配置
    source "$CONFIG_FILE" 2>/dev/null
    
    log_success "安全监控初始化完成"
    log_info "日志目录: $LOG_DIR"
    log_info "检查间隔: ${CHECK_INTERVAL}秒"
    log_info "报警阈值: ${ALERT_THRESHOLD}次"
}

# 创建配置文件
create_config_file() {
    cat > "$CONFIG_FILE" << EOF
# 安全监控配置文件
# 修改后需要重启监控脚本

# 监控配置
CHECK_INTERVAL=60           # 检查间隔（秒）
ALERT_THRESHOLD=10          # 报警阈值
LOG_RETENTION_DAYS=30       # 日志保留天数

# 日志文件路径（支持通配符）
LOG_FILES=(
    "/var/log/nginx/access.log"
    "/var/log/nginx/error.log"
    "/var/log/auth.log"
    "/var/log/syslog"
    "/var/log/messages"
    "logs/*.log"
    "backend/logs/*.log"
)

# 监控的IP地址（白名单）
TRUSTED_IPS=(
    "127.0.0.1"
    "192.168.1.0/24"
    "10.0.0.0/8"
)

# 监控的关键文件
MONITOR_FILES=(
    "/etc/passwd"
    "/etc/shadow"
    "/etc/hosts"
    "application.yml"
    "backend/src/main/resources/application.yml"
)

# 报警方式配置
ENABLE_EMAIL_ALERT=false
EMAIL_RECIPIENTS="admin@example.com"
ENABLE_SLACK_ALERT=false
SLACK_WEBHOOK_URL=""
ENABLE_SMS_ALERT=false
SMS_NUMBERS=""

# 高级配置
ENABLE_REAL_TIME_MONITOR=true
ENABLE_ATTACK_BLOCKING=false
BLOCK_DURATION=3600  # 封锁时长（秒）
EOF
    
    log_info "配置文件已创建: $CONFIG_FILE"
}

# 监控日志文件
monitor_logs() {
    log_info "开始监控日志文件..."
    
    # 获取日志文件列表
    local log_files=()
    if [ ${#LOG_FILES[@]} -gt 0 ]; then
        log_files=("${LOG_FILES[@]}")
    else
        # 默认日志文件
        log_files=(
            "/var/log/nginx/access.log"
            "/var/log/nginx/error.log"
            "/var/log/auth.log"
            "logs/application.log"
        )
    fi
    
    # 检查每个日志文件
    for log_file in "${log_files[@]}"; do
        if [ -f "$log_file" ]; then
            monitor_single_log "$log_file"
        elif [ -d "$(dirname "$log_file")" ]; then
            # 支持通配符
            for file in $log_file; do
                if [ -f "$file" ]; then
                    monitor_single_log "$file"
                fi
            done
        else
            log_warning "日志文件不存在: $log_file"
        fi
    done
}

# 监控单个日志文件
monitor_single_log() {
    local log_file=$1
    local log_name=$(basename "$log_file")
    
    log_info "监控日志文件: $log_file"
    
    # 检查文件大小
    local file_size=$(stat -c%s "$log_file" 2>/dev/null || stat -f%z "$log_file" 2>/dev/null)
    if [ "$file_size" -gt 104857600 ]; then  # 大于100MB
        log_warning "日志文件过大: $log_file (${file_size}字节)"
    fi
    
    # 检查最后修改时间
    local last_modified=$(stat -c%Y "$log_file" 2>/dev/null || stat -f%m "$log_file" 2>/dev/null)
    local current_time=$(date +%s)
    local time_diff=$((current_time - last_modified))
    
    if [ "$time_diff" -gt 3600 ]; then  # 1小时未更新
        log_warning "日志文件长时间未更新: $log_file"
    fi
    
    # 分析日志内容
    analyze_log_content "$log_file" "$log_name"
}

# 分析日志内容
analyze_log_content() {
    local log_file=$1
    local log_name=$2
    
    # 创建分析结果文件
    local analysis_file="$LOG_DIR/analysis_${log_name}_$(date +%Y%m%d).txt"
    
    # 检查各种攻击模式
    for attack_type in "${!ATTACK_PATTERNS[@]}"; do
        local pattern="${ATTACK_PATTERNS[$attack_type]}"
        local count=$(grep -E -i "$pattern" "$log_file" 2>/dev/null | wc -l)
        
        if [ "$count" -gt 0 ]; then
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] $attack_type: 发现 $count 个匹配项" >> "$analysis_file"
            
            if [ "$count" -gt "$ALERT_THRESHOLD" ]; then
                log_alert "检测到 $attack_type 攻击！文件: $log_file, 匹配数: $count"
                
                # 记录详细信息
                grep -E -i "$pattern" "$log_file" 2>/dev/null | head -20 >> "$LOG_DIR/${attack_type}_details_$(date +%Y%m%d).txt"
            else
                log_warning "检测到 $attack_type 模式: $log_file (${count}个)"
            fi
        fi
    done
    
    # 检查异常状态码
    check_status_codes "$log_file" "$analysis_file"
    
    # 检查异常IP
    check_suspicious_ips "$log_file" "$analysis_file"
    
    # 检查请求频率
    check_request_frequency "$log_file" "$analysis_file"
}

# 检查状态码
check_status_codes() {
    local log_file=$1
    local analysis_file=$2
    
    # 检查5xx错误
    local server_errors=$(grep -E '" 5[0-9]{2} ' "$log_file" 2>/dev/null | wc -l)
    if [ "$server_errors" -gt 0 ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] SERVER_ERRORS: 发现 $server_errors 个5xx错误" >> "$analysis_file"
        
        if [ "$server_errors" -gt "$ALERT_THRESHOLD" ]; then
            log_alert "检测到大量服务器错误！文件: $log_file, 错误数: $server_errors"
        fi
    fi
    
    # 检查4xx错误
    local client_errors=$(grep -E '" 4[0-9]{2} ' "$log_file" 2>/dev/null | wc -l)
    if [ "$client_errors" -gt 0 ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] CLIENT_ERRORS: 发现 $client_errors 个4xx错误" >> "$analysis_file"
        
        if [ "$client_errors" -gt "$((ALERT_THRESHOLD * 5))" ]; then
            log_alert "检测到大量客户端错误！可能正在被扫描，文件: $log_file, 错误数: $client_errors"
        fi
    fi
    
    # 检查404错误（单独统计）
    local not_found=$(grep -E '" 404 ' "$log_file" 2>/dev/null | wc -l)
    if [ "$not_found" -gt "$ALERT_THRESHOLD" ]; then
        log_warning "检测到大量404错误: $log_file (${not_found}个)"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] NOT_FOUND_404: 发现 $not_found 个404错误" >> "$analysis_file"
    fi
}

# 检查可疑IP
check_suspicious_ips() {
    local log_file=$1
    local analysis_file=$2
    
    # 提取IP地址
    local ip_list=$(grep -o -E '([0-9]{1,3}\.){3}[0-9]{1,3}' "$log_file" 2>/dev/null | sort | uniq -c | sort -nr)
    
    if [ -n "$ip_list" ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] IP_ANALYSIS:" >> "$analysis_file"
        echo "$ip_list" | head -20 >> "$analysis_file"
        
        # 检查单个IP的请求频率
        while read -r line; do
            local count=$(echo "$line" | awk '{print $1}')
            local ip=$(echo "$line" | awk '{print $2}')
            
            if [ "$count" -gt "$((ALERT_THRESHOLD * 10))" ]; then
                log_alert "检测到高频请求IP: $ip, 请求次数: $count"
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] HIGH_FREQUENCY_IP: $ip ($count次请求)" >> "$LOG_DIR/suspicious_ips_$(date +%Y%m%d).txt"
            fi
        done <<< "$ip_list"
    fi
}

# 检查请求频率
check_request_frequency() {
    local log_file=$1
    local analysis_file=$2
    
    # 按分钟统计请求数
    local last_minute=$(date -d "1 minute ago" '+%d/%b/%Y:%H:%M')
    local minute_count=$(grep -c "$last_minute" "$log_file" 2>/dev/null)
    
    if [ "$minute_count" -gt "$((ALERT_THRESHOLD * 5))" ]; then
        log_alert "检测到高频率请求！上一分钟请求数: $minute_count"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] HIGH_REQUEST_RATE: 上一分钟 $minute_count 次请求" >> "$analysis_file"
    fi
}

# 监控系统资源
monitor_system_resources() {
    log_info "监控系统资源..."
    
    # CPU使用率
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    if [ "${cpu_usage%.*}" -gt 80 ]; then
        log_warning "CPU使用率过高: ${cpu_usage}%"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] HIGH_CPU_USAGE: ${cpu_usage}%" >> "$LOG_DIR/system_monitor_$(date +%Y%m%d).txt"
    fi
    
    # 内存使用率
    local mem_usage=$(free | grep Mem | awk '{print $3/$2 * 100.0}')
    if [ "${mem_usage%.*}" -gt 80 ]; then
        log_warning "内存使用率过高: ${mem_usage}%"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] HIGH_MEMORY_USAGE: ${mem_usage}%" >> "$LOG_DIR/system_monitor_$(date +%Y%m%d).txt"
    fi
    
    # 磁盘使用率
    local disk_usage=$(df -h / | awk 'NR==2 {print $5}' | cut -d'%' -f1)
    if [ "$disk_usage" -gt 80 ]; then
        log_warning "磁盘使用率过高: ${disk_usage}%"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] HIGH_DISK_USAGE: ${disk_usage}%" >> "$LOG_DIR/system_monitor_$(date +%Y%m%d).txt"
    fi
    
    # 网络连接数
    local connections=$(netstat -an | grep -c ESTABLISHED)
    if [ "$connections" -gt 1000 ]; then
        log_warning "网络连接数过高: ${connections}"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] HIGH_CONNECTIONS: ${connections}个连接" >> "$LOG_DIR/system_monitor_$(date +%Y%m%d).txt"
    fi
}

# 监控关键文件
monitor_critical_files() {
    log_info "监控关键文件..."
    
    # 获取监控文件列表
    local monitor_files=()
    if [ ${#MONITOR_FILES[@]} -gt 0 ]; then
        monitor_files=("${MONITOR_FILES[@]}")
    else
        # 默认监控文件
        monitor_files=(
            "/etc/passwd"
            "/etc/shadow"
            "application.yml"
            "backend/src/main/resources/application.yml"
        )
    fi
    
    for file in "${monitor_files[@]}"; do
        if [ -f "$file" ]; then
            monitor_single_file "$file"
        fi
    done
}

# 监控单个文件
monitor_single_file() {
    local file=$1
    local file_name=$(basename "$file")
    local monitor_file="$LOG_DIR/file_monitor_${file_name}.txt"
    
    # 检查文件权限
    local permissions=$(stat -c%a "$file" 2>/dev/null || stat -f%p "$file" 2>/dev/null)
    if [ "${permissions: -1}" -gt 4 ]; then  # 世界可读
        log_warning "文件权限过松: $file (权限: $permissions)"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] LOOSE_PERMISSIONS: $file ($permissions)" >> "$monitor_file"
    fi
    
    # 检查文件大小变化
    if [ -f "$monitor_file" ]; then
        local last_size=$(grep "FILE_SIZE" "$monitor_file" | tail -1 | awk '{print $3}')
        local current_size=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null)
        
        if [ -n "$last_size" ] && [ "$last_size" -ne "$current_size" ]; then
            log_warning "文件大小变化: $file (从 ${last_size}字节 变为 ${current_size}字节)"
        fi
    fi
    
    # 记录当前状态
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] FILE_SIZE: $current_size" >> "$monitor_file"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] PERMISSIONS: $permissions" >> "$monitor_file"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] LAST_MODIFIED: $(stat -c%y "$file" 2>/dev/null || stat -f%Sm "$file" 2>/dev/null)" >> "$monitor_file"
}

# 生成监控报告
generate_monitor_report() {
    log_info "生成监控报告..."
    
    local report_file="$LOG_DIR/monitor_report_$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$report_file" << EOF
# 安全监控报告
生成时间: $(date)

## 监控概况
- 监控开始时间: $MONITOR_START_TIME
- 监控结束时间: $(date)
- 检查间隔: ${CHECK_INTERVAL}秒
- 报警阈值: ${ALERT_THRESHOLD}次

## 安全事件统计

### 1. 攻击检测统计
EOF

    # 统计各种攻击类型
    for attack_type in "${!ATTACK_PATTERNS[@]}"; do
        local count=$(grep -c "$attack_type" "$LOG_DIR"/analysis_*.txt 2>/dev/null || echo "0")
        if [ "$count" -gt 0 ]; then
            echo "- $attack_type: $count 次检测" >> "$report_file"
        fi
    done

    cat >> "$report_file" << EOF

### 2. 报警统计
- 总报警次数: $(grep -c "🚨 \[ALERT\]" security_monitor.log 2>/dev/null || echo "0")
- 警告次数: $(grep -c "\[WARNING\]" security_monitor.log 2>/dev/null || echo "0")

### 3. 系统资源统计
EOF

    # 添加系统资源信息
    if [ -f "$LOG_DIR/system_monitor_$(date +%Y%m%d).txt" ]; then
        grep "HIGH_" "$LOG_DIR/system_monitor_$(date +%Y%m%d).txt" 2>/dev/null >> "$report_file"
    fi

    cat >> "$report_file" << EOF

## 检测到的威胁

### 1. 高风险威胁
$(grep "🚨 \[ALERT\]" security_monitor.log 2>/dev/null | tail -10 | sed 's/.*🚨 \[ALERT\] //')

### 2. 中风险威胁
$(grep "\[WARNING\]" security_monitor.log 2>/dev/null | grep -v "🚨" | tail -10 | sed 's/.*\[WARNING\] //')

## 可疑活动

### 1. 可疑IP地址
$(cat "$LOG_DIR/suspicious_ips_$(date +%Y%m%d).txt" 2>/dev/null | head -10)

### 2. 异常请求模式
$(grep "HIGH_REQUEST_RATE\|HIGH_FREQUENCY_IP" "$LOG_DIR"/analysis_*.txt 2>/dev/null | tail -10)

## 系统状态

### 1. 资源使用情况
- CPU使用率: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%
- 内存使用率: $(free | grep Mem | awk '{print $3/$2 * 100.0}')%
- 磁盘使用率: $(df -h / | awk 'NR==2 {print $5}')

### 2. 网络状态
- 活跃连接数: $(netstat -an | grep -c ESTABLISHED)

## 建议措施

### 1. 立即行动
- 调查所有高风险报警
- 封锁恶意IP地址
- 检查受影响的服务

### 2. 短期改进
- 调整安全策略
- 优化监控规则
- 加强日志记录

### 3. 长期规划
- 实施WAF防护
- 建立SOC团队
- 定期安全审计

## 附录

### 监控配置
- 检查间隔: ${CHECK_INTERVAL}秒
- 报警阈值: ${ALERT_THRESHOLD}次
- 日志保留: ${LOG_RETENTION_DAYS:-30}天

### 监控范围
- 日志文件: ${#LOG_FILES[@]} 个
- 关键文件: ${#MONITOR_FILES[@]} 个
- 攻击模式: ${#ATTACK_PATTERNS[@]} 种

---
*报告生成系统: 燕小图安全监控工具*
*版本: 1.0*
EOF

    log_success "监控报告生成完成: $report_file"
}

# 清理旧日志
cleanup_old_logs() {
    log_info "清理旧日志..."
    
    local retention_days=${LOG_RETENTION_DAYS:-30}
    find "$LOG_DIR" -name "*.txt" -mtime +$retention_days -delete 2>/dev/null
    find "$LOG_DIR" -name "*.log" -mtime +$retention_days -delete 2>/dev/null
    
    log_success "已清理超过 ${retention_days} 天的旧日志"
}

# 主监控循环
monitor_loop() {
    log_info "进入监控循环，按 Ctrl+C 停止..."
    
    MONITOR_START_TIME=$(date)
    
    while true; do
        log_info "开始新一轮监控检查..."
        
        # 监控日志文件
        monitor_logs
        
        # 监控系统资源
        monitor_system_resources
        
        # 监控关键文件
        monitor_critical_files
        
        # 生成报告（每小时一次）
        if [ "$(date +%M)" = "00" ]; then
            generate_monitor_report
            cleanup_old_logs
        fi
        
        log_info "监控检查完成，等待 ${CHECK_INTERVAL} 秒..."
        sleep "$CHECK_INTERVAL"
    done
}

# 主函数
main() {
    echo "========================================="
    echo "      燕小图安全监控工具 v1.0            "
    echo "========================================="
    
    # 捕获退出信号
    trap 'log_info "监控停止"; generate_monitor_report; exit 0' INT TERM
    
    # 初始化
    init_monitor
    
    # 启动监控循环
    monitor_loop
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
  start     启动监控（默认）
  stop      停止监控
  status    查看监控状态
  report    生成监控报告
  config    编辑配置文件
  -h, --help  显示此帮助信息

命令:
  start     启动安全监控
  stop      停止安全监控
  status    查看监控状态和统计
  report    生成详细的监控报告
  config    编辑监控配置文件

示例:
  $0 start           # 启动安全监控
  $0 status          # 查看监控状态
  $0 report          # 生成监控报告
  $0 config          # 编辑配置文件
  $0                 # 启动监控（默认）

功能:
  1. 实时监控日志文件，检测攻击行为
  2. 监控系统资源使用情况
  3. 监控关键文件变化
  4. 自动生成安全报告
  5. 支持多种报警方式

检测的攻击类型:
  - SQL注入攻击
  - XSS跨站脚本攻击
  - 暴力破解攻击
  - CSRF跨站请求伪造
  - 路径遍历攻击
  - 命令注入攻击
  - 文件包含攻击
  - DDoS攻击
  - 端口扫描
  - 未授权访问

注意:
  1. 需要管理员权限运行
  2. 建议配置报警通知
  3. 定期查看监控报告
  4. 根据实际情况调整配置
EOF
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # 解析命令
    case "${1:-start}" in
        start)
            main
            ;;
        stop)
            log_info "停止监控功能需要实现进程管理"
            ;;
        status)
            log_info "监控状态功能需要实现状态检查"
            ;;
        report)
            init_monitor
            generate_monitor_report
            ;;
        config)
            ${EDITOR:-vi} "$CONFIG_FILE"
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "未知命令: $1"
            show_help
            exit 1
            ;;
    esac
fi