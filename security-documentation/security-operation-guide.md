# 安全操作指南

## 概述
本文档为"研校地图"网站的安全运维人员提供操作指南，包括日常安全维护、应急响应、安全监控等操作流程和最佳实践。

## 1. 日常安全维护

### 1.1 安全配置检查
**频率**: 每日  
**操作步骤**:
1. 检查安全策略配置文件状态
   ```bash
   # 检查security-policy.yml配置
   cat backend/src/main/resources/security/security-policy.yml | grep -E "(enabled|status)" | grep -v "true"
   ```
2. 验证IP黑名单更新
   ```bash
   # 检查黑名单IP数量
   wc -l backend/src/main/resources/security/blacklist-ips.txt
   ```
3. 检查敏感词过滤列表
   ```bash
   # 验证敏感词文件完整性
   md5sum backend/src/main/resources/security/sensitive-words.txt
   ```

**预期结果**: 配置文件完整，黑名单和敏感词列表正常更新

### 1.2 安全日志审查
**频率**: 每日  
**操作步骤**:
1. 查看安全事件日志
   ```bash
   # 查看今日安全日志（示例命令，实际路径根据日志配置）
   tail -n 100 /var/log/application/security.log | grep -E "(FAIL|ERROR|ATTACK)"
   ```
2. 分析失败登录尝试
   ```bash
   # 统计失败登录次数
   grep "登录失败" /var/log/application/security.log | wc -l
   ```
3. 检查异常访问模式
   ```bash
   # 检查高频访问IP
   awk '{print $1}' /var/log/application/access.log | sort | uniq -c | sort -nr | head -10
   ```

**预期结果**: 无异常安全事件，失败登录在正常范围内

### 1.3 漏洞扫描
**频率**: 每周  
**操作步骤**:
1. 运行自动化漏洞扫描脚本
   ```bash
   # 执行漏洞扫描
   bash scripts/security/scan-vulnerabilities.sh
   ```
2. 分析扫描报告
   ```bash
   # 查看扫描结果
   cat vulnerability-report-$(date +%Y%m%d).json | jq '.vulnerabilities[] | select(.severity=="HIGH")'
   ```
3. 跟踪漏洞修复状态
   ```bash
   # 检查未修复的高危漏洞
   grep "状态:未修复" vulnerability-report-$(date +%Y%m%d).md
   ```

**预期结果**: 无高危漏洞，中低危漏洞有修复计划

## 2. 应急响应流程

### 2.1 安全事件分类
| 事件级别 | 描述 | 响应时间要求 |
|---------|------|-------------|
| **严重** | 数据泄露、系统入侵、服务中断 | 立即响应（<15分钟） |
| **高危** | 高危漏洞被利用、大规模攻击 | 紧急响应（<1小时） |
| **中危** | 中危漏洞、异常访问模式 | 优先处理（<24小时） |
| **低危** | 低危漏洞、配置问题 | 计划处理（<7天） |

### 2.2 应急响应步骤
#### 步骤1: 事件确认
1. 收集事件信息（时间、IP、影响范围）
2. 验证事件真实性（排除误报）
3. 确定事件级别

#### 步骤2: 初步控制
1. **隔离影响**: 临时封禁攻击IP
   ```bash
   # 添加IP到黑名单
   echo "攻击IP $(date)" >> backend/src/main/resources/security/blacklist-ips.txt
   ```
2. **停止扩散**: 暂停受影响服务
3. **保护证据**: 备份相关日志和数据

#### 步骤3: 深入分析
1. 分析攻击路径和手法
2. 评估数据泄露风险
3. 确定根本原因

#### 步骤4: 恢复处理
1. 修复安全漏洞
2. 清除后门和恶意代码
3. 恢复服务和数据

#### 步骤5: 事后总结
1. 编写事件报告
2. 更新安全策略
3. 进行安全培训

### 2.3 常见事件处理

#### 2.3.1 暴力破解攻击
**症状**: 大量失败登录尝试  
**处理**:
1. 临时封禁攻击IP
   ```bash
   # 自动封禁失败登录超过10次的IP
   bash scripts/security/monitor-attacks.sh --action=block-ip --threshold=10
   ```
2. 启用验证码机制
3. 检查账户安全状态

#### 2.3.2 SQL注入尝试
**症状**: 日志中出现SQL关键字  
**处理**:
1. 分析攻击payload
2. 验证参数化查询有效性
3. 更新WAF规则

#### 2.3.3 XSS攻击尝试
**症状**: 输入中包含恶意脚本  
**处理**:
1. 验证输入过滤规则
2. 检查输出编码实现
3. 更新CSP策略

#### 2.3.4 数据泄露事件
**症状**: 异常数据访问、敏感数据暴露  
**处理**:
1. 立即隔离受影响系统
2. 评估泄露数据范围和影响
3. 通知相关方（如涉及个人信息）
4. 报告监管部门（如法律要求）

## 3. 安全监控操作

### 3.1 实时监控
**监控工具**: 攻击监控脚本  
**操作命令**:
```bash
# 启动实时监控
bash scripts/security/monitor-attacks.sh --mode=realtime --alert=true
```

**监控指标**:
- 请求频率异常
- SQL注入特征
- XSS攻击特征
- 敏感路径访问
- 文件上传异常

### 3.2 阈值配置
**配置文件**: `scripts/security/monitor-attacks.sh`中的阈值参数  
**可配置项**:
```bash
# 攻击检测阈值
SQL_INJECTION_THRESHOLD=5    # SQL注入尝试次数
XSS_ATTACK_THRESHOLD=5       # XSS攻击尝试次数
BRUTE_FORCE_THRESHOLD=10     # 暴力破解尝试次数
UPLOAD_ATTACK_THRESHOLD=3    # 文件上传攻击次数
```

### 3.3 告警处理
**告警渠道**:
- 邮件告警: security-alert@yanxiaomap.com
- 短信告警: 运维人员手机
- 即时通讯: 企业微信/钉钉群

**告警确认流程**:
1. 收到告警后15分钟内确认
2. 判断告警级别
3. 启动相应应急流程

## 4. 合规管理操作

### 4.1 合规检查
**频率**: 每月  
**操作步骤**:
1. 运行合规检查脚本
   ```bash
   bash scripts/security/check-compliance.sh --report=html
   ```
2. 检查备案状态
   ```bash
   # 调用合规API检查备案状态
   curl -X GET "http://localhost:8080/api/compliance/filing-info" | jq '.data'
   ```
3. 验证隐私政策
   ```bash
   # 检查隐私政策版本
   curl -X GET "http://localhost:8080/api/compliance/privacy-policy/latest" | jq '.data.version'
   ```

### 4.2 合规报告
**报告周期**: 每季度  
**报告内容**:
1. 安全事件统计
2. 漏洞修复情况
3. 合规检查结果
4. 改进建议

**报告模板**: 见`compliance-report-template.md`

## 5. 安全配置管理

### 5.1 配置文件管理
**安全配置文件位置**:
- `backend/src/main/resources/security/security-policy.yml`
- `backend/src/main/resources/security/sensitive-words.txt`
- `backend/src/main/resources/security/blacklist-ips.txt`

**备份策略**:
- 每日自动备份到安全存储
- 版本控制（Git）
- 加密存储敏感配置

### 5.2 密钥管理
**密钥类型**:
- JWT签名密钥
- 数据库密码
- API密钥（高德地图等）
- SSL证书私钥

**轮换策略**:
- JWT密钥: 90天
- 数据库密码: 180天
- API密钥: 按供应商要求
- SSL证书: 证书到期前30天

**安全存储**:
- 环境变量（生产环境）
- 密钥管理服务（如Vault）
- 加密配置文件（开发环境）

## 6. 安全工具使用

### 6.1 漏洞扫描工具
**脚本**: `scripts/security/scan-vulnerabilities.sh`  
**使用方法**:
```bash
# 完整扫描
bash scripts/security/scan-vulnerabilities.sh --full

# 快速扫描
bash scripts/security/scan-vulnerabilities.sh --quick

# 指定目标扫描
bash scripts/security/scan-vulnerabilities.sh --target=backend
```

**输出文件**:
- `reports/vulnerability-YYYYMMDD.json` - JSON格式报告
- `reports/vulnerability-YYYYMMDD.md` - Markdown格式报告
- `reports/vulnerability-YYYYMMDD.html` - HTML格式报告

### 6.2 合规检查工具
**脚本**: `scripts/security/check-compliance.sh`  
**使用方法**:
```bash
# 检查所有合规项
bash scripts/security/check-compliance.sh --all

# 检查特定标准
bash scripts/security/check-compliance.sh --standard=gdpr
bash scripts/security/check-compliance.sh --standard=mlps2.0

# 生成详细报告
bash scripts/security/check-compliance.sh --verbose --report=detailed
```

### 6.3 攻击监控工具
**脚本**: `scripts/security/monitor-attacks.sh`  
**运行模式**:
```bash
# 实时监控模式
bash scripts/security/monitor-attacks.sh --mode=realtime

# 历史日志分析
bash scripts/security/monitor-attacks.sh --mode=historical --days=7

# 测试模式
bash scripts/security/monitor-attacks.sh --mode=test --sample=1000
```

## 7. 培训与演练

### 7.1 安全培训计划
**培训对象**:
- 开发人员: 安全编码、漏洞修复
- 运维人员: 安全配置、应急响应
- 管理人员: 安全策略、合规要求

**培训频率**: 每半年一次

### 7.2 应急演练
**演练类型**:
- 桌面推演: 每季度
- 实战演练: 每半年
- 红蓝对抗: 每年

**演练场景**:
1. DDoS攻击应急
2. 数据泄露处置
3. 网站篡改恢复
4. 供应链攻击应对

## 8. 附录

### 8.1 常用命令速查
```bash
# 安全日志查看
tail -f /var/log/application/security.log
grep "攻击" /var/log/application/*.log
journalctl -u application --since "1 hour ago" | grep -i security

# IP封禁管理
ipset list blacklist
iptables -L -n | grep DROP

# 证书检查
openssl x509 -in certificate.crt -text -noout
sslscan example.com:443

# 端口扫描检查
netstat -tulpn | grep LISTEN
ss -tulpn
```

### 8.2 联系信息
**安全团队**:
- 安全负责人: security@yanxiaomap.com
- 应急联系人: emergency@yanxiaomap.com
- 合规联系人: compliance@yanxiaomap.com

**外部支持**:
- 公安网安部门: 110
- 网络安全应急响应中心: CNCERT
- 漏洞报告平台: 补天、漏洞盒子

### 8.3 相关文档
- `security-policy.md` - 安全策略文档
- `compliance-report-template.md` - 合规报告模板
- `incident-response-plan.md` - 应急响应计划
- `security-configuration-guide.md` - 安全配置指南

---

**文档版本**: 1.0  
**最后更新**: 2026-04-18  
**下次审查**: 2026-10-18  
**负责人**: 安全运维团队