# 安全事件应急响应计划

## 1. 概述

### 1.1 目的
本应急响应计划旨在为"研校地图"网站安全事件提供系统化、规范化的响应流程，确保在发生安全事件时能够迅速、有效地控制事件影响，恢复系统正常运行，减少业务损失。

### 1.2 适用范围
本计划适用于以下安全事件：
- 网络攻击事件（DDoS、SQL注入、XSS、CSRF等）
- 系统入侵事件（未授权访问、权限提升）
- 数据安全事件（数据泄露、数据篡改、数据丢失）
- 恶意代码事件（病毒、木马、勒索软件）
- 账户安全事件（暴力破解、凭证泄露）
- 合规安全事件（备案失效、隐私违规）

### 1.3 响应原则
1. **快速响应原则**: 发现事件后立即启动应急响应
2. **最小影响原则**: 采取对业务影响最小的处置措施
3. **证据保护原则**: 保护现场证据，便于事后分析
4. **信息保密原则**: 控制信息知悉范围，防止恐慌扩散
5. **持续改进原则**: 事后总结分析，完善安全防护

## 2. 应急响应组织

### 2.1 应急响应小组
| 角色 | 职责 | 联系人 | 备份联系人 |
|------|------|--------|------------|
| **应急指挥** | 总体决策、资源协调 | 项目经理 | 技术负责人 |
| **技术负责人** | 技术方案制定、执行监督 | 架构师 | 开发负责人 |
| **安全专家** | 安全分析、漏洞排查 | 安全工程师 | 运维工程师 |
| **运维人员** | 系统操作、日志分析 | 运维工程师 | 开发工程师 |
| **法律顾问** | 法律合规、外部沟通 | 法务专员 | 合规专员 |
| **公关人员** | 对外公告、用户沟通 | 市场专员 | 客服主管 |

### 2.2 联系方式
```yaml
应急响应通讯录:
  紧急联系电话:
    - 应急指挥: 138-XXXX-XXXX
    - 技术负责人: 139-XXXX-XXXX
    - 安全专家: 136-XXXX-XXXX
  
  紧急联系群组:
    - 企业微信应急群: "研校地图安全应急"
    - 钉钉应急群: "研校地图安全事件响应"
    - 电话会议桥: 400-XXX-XXX 密码: XXXX
  
  外部联系单位:
    - 公安网安部门: 110
    - CNCERT: 010-XXXXXXXX
    - 云服务商安全支持: 400-XXX-XXXX
```

## 3. 事件分类与分级

### 3.1 事件分类
| 类别 | 子类 | 示例 |
|------|------|------|
| **网络攻击** | DDoS攻击、端口扫描、暴力破解 | SYN Flood、端口扫描、登录爆破 |
| **应用攻击** | SQL注入、XSS、CSRF、文件上传漏洞 | SQL注入尝试、XSS攻击、恶意文件上传 |
| **系统入侵** | 未授权访问、权限提升、后门程序 | 越权访问、root提权、webshell |
| **数据安全** | 数据泄露、数据篡改、数据丢失 | 数据库泄露、数据被篡改、备份丢失 |
| **恶意代码** | 病毒、木马、勒索软件、挖矿程序 | 勒索病毒、挖矿木马、蠕虫病毒 |
| **账户安全** | 凭证泄露、会话劫持、冒用身份 | 密码泄露、会话被盗、身份冒用 |
| **合规事件** | 备案失效、隐私违规、内容违规 | ICP备案过期、隐私数据违规收集 |

### 3.2 事件分级
| 级别 | 名称 | 影响程度 | 响应时限 | 通知范围 |
|------|------|----------|----------|----------|
| **一级** | 特别重大 | 核心系统瘫痪、大规模数据泄露 | 立即响应 (<15分钟) | 公司管理层、相关部门、监管机构 |
| **二级** | 重大 | 重要功能受损、敏感数据泄露 | 紧急响应 (<1小时) | 应急小组、相关部门 |
| **三级** | 较大 | 一般功能受损、少量数据影响 | 快速响应 (<4小时) | 应急小组、技术团队 |
| **四级** | 一般 | 轻微影响、无数据泄露 | 计划响应 (<24小时) | 技术团队、运维团队 |

## 4. 应急响应流程

### 4.1 整体流程图
```
事件发现 → 初步评估 → 事件定级 → 启动响应 → 初步控制
    ↓
证据收集 → 深入分析 → 恢复处理 → 系统恢复
    ↓
事件总结 → 改进措施 → 文档归档
```

### 4.2 阶段一：准备与检测

#### 4.2.1 日常准备
**监控系统配置**:
```bash
# 安全监控脚本（实时运行）
bash scripts/security/monitor-attacks.sh --mode=realtime --alert=true

# 日志监控（关键指标）
tail -f /var/log/application/security.log | grep -E "(ERROR|ATTACK|FAIL)"
```

**工具准备**:
- 网络抓包工具: tcpdump, Wireshark
- 日志分析工具: ELK Stack, Splunk
- 恶意代码分析工具: VirusTotal, YARA
- 系统取证工具: Autopsy, FTK Imager

#### 4.2.2 事件检测
**检测来源**:
1. **主动监控告警**
   - 安全监控脚本告警
   - IDS/IPS系统告警
   - WAF防护告警
   - 日志异常告警

2. **被动发现**
   - 用户报告异常
   - 第三方通报
   - 安全厂商预警
   - 监管机构通知

**检测确认**:
```bash
# 确认攻击事件脚本
#!/bin/bash
# confirm-attack.sh

ATTACK_TYPE=$1
ATTACK_IP=$2

echo "=== 攻击事件确认检查 ==="

# 1. 检查攻击IP历史记录
echo "1. 检查IP历史记录..."
grep "$ATTACK_IP" /var/log/application/*.log | head -5

# 2. 检查当前连接
echo "2. 检查当前连接..."
netstat -tunap | grep "$ATTACK_IP"

# 3. 检查防火墙规则
echo "3. 检查防火墙规则..."
iptables -L -n | grep "$ATTACK_IP"

# 4. 检查黑名单
echo "4. 检查黑名单状态..."
grep "$ATTACK_IP" backend/src/main/resources/security/blacklist-ips.txt

echo "=== 检查完成 ==="
```

### 4.3 阶段二：分析与定级

#### 4.3.1 初步分析
**信息收集清单**:
- 事件发生时间
- 影响系统/服务
- 攻击源IP/端口
- 攻击手法/特征
- 受影响用户范围
- 当前状态（进行中/已结束）

**初步分析脚本**:
```bash
#!/bin/bash
# initial-analysis.sh

INCIDENT_TIME=$1
ATTACK_IP=$2

echo "=== 初步分析报告 ==="
echo "分析时间: $(date)"
echo "事件时间: $INCIDENT_TIME"
echo "攻击IP: $ATTACK_IP"

# 收集相关信息
echo "## 1. 攻击时间段日志"
grep "$INCIDENT_TIME" /var/log/application/access.log | head -10

echo "## 2. 攻击IP相关日志"
find /var/log -name "*.log" -exec grep -l "$ATTACK_IP" {} \; | head -5

echo "## 3. 系统状态检查"
top -b -n 1 | head -20
free -h

echo "## 4. 网络连接检查"
ss -tunap | grep -E "(ESTAB|LISTEN)" | head -20

echo "=== 分析完成 ==="
```

#### 4.3.2 事件定级
**定级决策矩阵**:
| 影响维度 | 一级（特别重大） | 二级（重大） | 三级（较大） | 四级（一般） |
|----------|------------------|--------------|--------------|--------------|
| **业务影响** | 核心业务完全中断 | 重要业务受损 | 一般业务受影响 | 业务轻微影响 |
| **数据影响** | 大规模敏感数据泄露 | 部分敏感数据泄露 | 非敏感数据泄露 | 无数据泄露 |
| **系统影响** | 多系统瘫痪 | 单系统瘫痪 | 系统性能下降 | 系统运行正常 |
| **用户影响** | 大量用户受影响 | 部分用户受影响 | 少数用户受影响 | 个别用户受影响 |
| **恢复时间** | >24小时 | 4-24小时 | 1-4小时 | <1小时 |

### 4.4 阶段三：控制与遏制

#### 4.4.1 初步控制措施
**立即执行的操作**:
1. **隔离攻击源**
   ```bash
   # 封禁攻击IP
   echo "$(date +%Y-%m-%d\ %H:%M:%S) $ATTACK_IP # 攻击封禁" >> backend/src/main/resources/security/blacklist-ips.txt
   
   # 应用防火墙规则
   iptables -A INPUT -s $ATTACK_IP -j DROP
   iptables -A OUTPUT -d $ATTACK_IP -j DROP
   ```

2. **暂停受影响服务**
   ```bash
   # 暂停受影响的应用服务
   systemctl stop application-affected-service
   
   # 或重启服务（如果安全）
   systemctl restart application-service
   ```

3. **保护现场证据**
   ```bash
   # 备份相关日志
   BACKUP_DIR="/backup/incident-$(date +%Y%m%d-%H%M%S)"
   mkdir -p $BACKUP_DIR
   
   cp /var/log/application/*.log $BACKUP_DIR/
   cp /var/log/nginx/*.log $BACKUP_DIR/
   cp /var/log/auth.log $BACKUP_DIR/
   
   # 创建证据清单
   find $BACKUP_DIR -type f -exec sha256sum {} \; > $BACKUP_DIR/checksums.txt
   ```

#### 4.4.2 分类控制措施
**针对DDoS攻击**:
```bash
# 1. 启用云服务商DDoS防护
# 2. 调整Nginx配置限制连接
cat > /etc/nginx/conf.d/ddos.conf << EOF
limit_conn_zone \$binary_remote_addr zone=perip:10m;
limit_conn_zone \$server_name zone=perserver:10m;

limit_conn perip 10;
limit_conn perserver 100;

limit_req_zone \$binary_remote_addr zone=reqlimit:10m rate=10r/s;
limit_req zone=reqlimit burst=20 nodelay;
EOF

# 3. 重启Nginx
nginx -t && nginx -s reload
```

**针对SQL注入攻击**:
```sql
-- 1. 检查数据库异常查询
SELECT * FROM mysql.general_log 
WHERE argument LIKE '%SELECT%FROM%users%WHERE%'
   OR argument LIKE '%UNION%SELECT%'
ORDER BY event_time DESC
LIMIT 10;

-- 2. 检查数据库用户权限
SHOW GRANTS FOR CURRENT_USER();

-- 3. 临时增加SQL审计
SET GLOBAL general_log = 'ON';
```

**针对数据泄露**:
```bash
# 1. 检查数据访问日志
grep "敏感数据表名" /var/log/application/security.log

# 2. 检查数据库备份完整性
mysqldump --databases 敏感数据库 --lock-tables > /tmp/紧急备份.sql

# 3. 临时限制数据访问
REVOKE SELECT ON 敏感表.* FROM '应用用户'@'%';
```

### 4.5 阶段四：根除与恢复

#### 4.5.1 根除措施
**彻底清除威胁**:
1. **修复安全漏洞**
   ```bash
   # 检查并应用安全补丁
   apt update && apt upgrade --security -y
   
   # 更新应用依赖
   cd /app/backend && ./mvnw dependency:check
   ```

2. **清除恶意代码**
   ```bash
   # 扫描恶意文件
   find /app -type f -name "*.php" -exec grep -l "eval(base64_decode" {} \;
   find /app -type f -name "*.jsp" -exec grep -l "Runtime.getRuntime()" {} \;
   
   # 使用杀毒软件扫描
   clamscan -r /app --remove=yes
   ```

3. **重置受影响凭证**
   ```bash
   # 重置数据库密码
   ALTER USER '应用用户'@'%' IDENTIFIED BY '新密码';
   
   # 重置API密钥
   # 更新高德地图API密钥
   # 更新其他第三方服务密钥
   ```

#### 4.5.2 恢复措施
**系统恢复步骤**:
1. **验证备份完整性**
   ```bash
   # 检查备份文件
   sha256sum /backup/latest-backup.tar.gz
   
   # 验证数据库备份
   mysql -e "USE 测试数据库; SELECT COUNT(*) FROM 测试表;"
   ```

2. **恢复系统服务**
   ```bash
   # 恢复应用服务
   systemctl start application-service
   
   # 验证服务状态
   curl -f http://localhost:8080/actuator/health
   
   # 逐步恢复业务功能
   # 1. 恢复只读功能
   # 2. 恢复写入功能
   # 3. 恢复外部接口
   ```

3. **安全加固**
   ```bash
   # 应用安全补丁
   ./scripts/security/scan-vulnerabilities.sh --fix
   
   # 更新安全配置
   cp /backup/secure-config/security-policy.yml backend/src/main/resources/security/
   
   # 重启安全服务
   systemctl restart security-services
   ```

### 4.6 阶段五：事后处理

#### 4.6.1 事件总结
**事件报告模板**:
```markdown
# 安全事件报告

## 事件基本信息
- 事件ID: INC-YYYYMMDD-001
- 事件类型: [类型]
- 事件级别: [级别]
- 发生时间: YYYY-MM-DD HH:MM:SS
- 发现时间: YYYY-MM-DD HH:MM:SS
- 响应启动时间: YYYY-MM-DD HH:MM:SS
- 恢复完成时间: YYYY-MM-DD HH:MM:SS

## 事件影响评估
### 业务影响
- 受影响系统: [系统列表]
- 影响时长: [小时]
- 用户影响: [用户数量]
- 经济损失: [估算金额]

### 技术影响
- 受影响组件: [组件列表]
- 数据影响: [数据范围]
- 系统完整性: [影响程度]

## 响应过程记录
### 检测与评估
[详细记录]

### 控制与遏制
[详细记录]

### 根除与恢复
[详细记录]

## 根本原因分析
### 直接原因
[技术原因]

### 根本原因
[管理原因、流程原因]

### 贡献因素
[其他相关因素]

## 经验教训
### 成功做法
[值得保持的做法]

### 改进点
[需要改进的地方]

### 行动计划
[具体改进措施和时间表]

## 附件
1. 证据文件清单
2. 日志分析报告
3. 技术分析报告
```

#### 4.6.2 改进措施
**短期改进**（1周内）:
1. 修复已发现的安全漏洞
2. 更新安全配置
3. 加强监控告警

**中期改进**（1月内）:
1. 完善安全策略
2. 开展安全培训
3. 更新应急预案

**长期改进**（3月内）:
1. 架构安全加固
2. 安全文化建设
3. 建立安全运营体系

## 5. 特定事件响应指南

### 5.1 DDoS攻击响应

#### 5.1.1 响应步骤
1. **确认攻击**
   ```bash
   # 检查网络流量
   iftop -i eth0
   
   # 检查连接数
   netstat -an | grep :80 | wc -l
   ```

2. **启动缓解**
   ```bash
   # 启用云服务商DDoS防护
   # 配置CDN清洗
   # 调整Nginx限流
   ```

3. **业务保障**
   ```bash
   # 启用静态缓存
   # 切换到维护页面
   # 优先保障核心业务
   ```

#### 5.1.2 恢复检查清单
- [ ] 攻击流量已降至正常水平
- [ ] 服务响应时间恢复正常
- [ ] 用户访问不受影响
- [ ] 监控告警已消除

### 5.2 数据泄露响应

#### 5.2.1 响应步骤
1. **确认泄露**
   ```bash
   # 检查数据访问日志
   grep "敏感数据" /var/log/application/*.log
   
   # 检查数据库查询日志
   mysqlbinlog /var/log/mysql/binlog.XXXXXX | grep -i "敏感表"
   ```

2. **控制泄露**
   ```bash
   # 封锁泄露点
   # 暂停数据访问
   # 备份相关证据
   ```

3. **通知与报告**
   ```bash
   # 内部通知管理层
   # 外部通知监管机构（如需要）
   # 通知受影响用户（如需要）
   ```

#### 5.2.2 合规要求
- **GDPR要求**: 72小时内报告监管机构
- **网络安全法**: 立即报告公安机关
- **等级保护**: 报告属地公安网安部门

### 5.3 勒索软件响应

#### 5.3.1 响应步骤
1. **隔离感染**
   ```bash
   # 断开网络连接
   ifdown eth0
   
   # 隔离感染主机
   iptables -A INPUT -s 感染主机IP -j DROP
   ```

2. **识别样本**
   ```bash
   # 收集勒索信
   # 识别勒索软件家族
   # 检查加密文件类型
   ```

3. **恢复数据**
   ```bash
   # 从备份恢复
   # 使用解密工具（如有）
   # 重建系统
   ```

#### 5.3.2 注意事项
- **不要支付赎金**: 支付不能保证恢复，可能助长犯罪
- **保留证据**: 保留所有勒索信息
- **寻求帮助**: 联系网络安全应急响应中心

## 6. 演练与培训

### 6.1 应急演练计划

#### 6.1.1 演练频率
- **桌面推演**: 每季度1次
- **实战演练**: 每半年1次
- **红蓝对抗**: 每年1次

#### 6.1.2 演练场景
```yaml
演练场景库:
  1. DDoS攻击演练:
    - 目标: 验证抗DDoS能力
    - 方式: 模拟流量攻击
    - 评估: 服务可用性
  
  2. 数据泄露演练:
    - 目标: 验证数据保护措施
    - 方式: 模拟数据泄露
    - 评估: 响应速度、合规性
  
  3. 勒索软件演练:
    - 目标: 验证备份恢复能力
    - 方式: 模拟文件加密
    - 评估: 恢复时间、数据完整性
  
  4. 供应链攻击演练:
    - 目标: 验证第三方风险管理
    - 方式: 模拟依赖库漏洞
    - 评估: 漏洞修复流程
```

### 6.2 培训计划

#### 6.2.1 培训内容
- **安全意识培训**: 所有员工，每年2次
- **技术培训**: 技术人员，每季度1次
- **应急响应培训**: 应急小组成员，每半年1次
- **合规培训**: 相关人员，每年1次

#### 6.2.2 培训材料
- 安全策略文档
- 应急响应手册
- 案例学习资料
- 工具使用指南

## 7. 附录

### 7.1 应急工具包
```bash
# 应急工具包准备脚本
#!/bin/bash
# prepare-emergency-kit.sh

KIT_DIR="/opt/emergency-kit"
mkdir -p $KIT_DIR/{tools,scripts,docs}

# 复制工具
cp /usr/bin/tcpdump $KIT_DIR/tools/
cp /usr/bin/wireshark $KIT_DIR/tools/
cp /usr/bin/clamscan $KIT_DIR/tools/

# 复制脚本
cp scripts/security/*.sh $KIT_DIR/scripts/

# 复制文档
cp security-documentation/*.md $KIT_DIR/docs/

# 创建使用指南
cat > $KIT_DIR/README.md << EOF
# 应急工具包使用指南

## 工具清单
1. tcpdump - 网络抓包
2. wireshark - 协议分析
3. clamscan - 病毒扫描

## 脚本清单
1. monitor-attacks.sh - 攻击监控
2. scan-vulnerabilities.sh - 漏洞扫描
3. check-compliance.sh - 合规检查

## 文档清单
1. incident-response-plan.md - 应急响应计划
2. security-operation-guide.md - 安全操作指南
3. security-configuration-guide.md - 安全配置指南
EOF

echo "应急工具包已准备到: $KIT_DIR"
```

### 7.2 外部资源
| 资源类型 | 名称 | 联系方式 | 用途 |
|----------|------|----------|------|
| **应急响应** | CNCERT | 010-XXXXXXXX | 国家级应急响应 |
| **公安网安** | 属地公安 | 110 | 案件报告 |
| **云服务商** | 阿里云安全 | 400-XXX-XXXX | DDoS防护、安全咨询 |
| **安全厂商** | 奇安信、深信服 | 厂商联系方式 | 技术支持、安全服务 |
| **法律咨询** | 律师事务所 | 律师联系方式 | 法律合规咨询 |

### 7.3 模板文件
- 事件报告模板
- 通知邮件模板
- 会议纪要模板
- 证据清单模板

### 7.4 版本历史
| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|----------|--------|
| 1.0 | 2026-04-18 | 初始版本 | 安全团队 |
| 1.1 | 2026-07-18 | 增加演练内容 | 安全团队 |

---

**计划生效日期**: 2026-04-18  
**计划审查周期**: 每半年审查一次  
**下次审查日期**: 2026-10-18  
**应急响应小组负责人**: [姓名]  
**批准人**: [姓名]