# 考研院校地图择校网站数据字典

## 文档信息
- **文档版本**: 1.0
- **创建日期**: 2026-04-17
- **更新日期**: 2026-04-17
- **数据库名称**: yanxiaomap
- **字符集**: UTF8MB4
- **排序规则**: utf8mb4_unicode_ci

## 表结构总览

| 序号 | 表名 | 中文名称 | 描述 | 记录数估算 |
|------|------|----------|------|------------|
| 1 | schools | 院校表 | 存储院校基本信息 | 1,000+ |
| 2 | subject_categories | 学科门类表 | 存储学科门类信息 | 20 |
| 3 | disciplines | 一级学科表 | 存储一级学科信息 | 50+ |
| 4 | majors | 专业表 | 存储专业信息 | 500+ |
| 5 | school_majors | 院校-专业关联表 | 院校与专业的关联关系 | 10,000+ |
| 6 | admission_data | 招生录取数据表 | 历年招生录取数据 | 50,000+ |
| 7 | admins | 管理员表 | 系统管理员账号 | 10 |
| 8 | data_change_logs | 数据修改记录表 | 数据修改操作日志 | 10,000+ |
| 9 | system_configs | 系统配置表 | 系统配置信息 | 50 |

## 详细字段说明

### 1. schools（院校表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 院校唯一ID | 自增主键 | 1 |
| name | varchar | 100 | NO | - | - | - | - | 院校全称 | 官方完整名称 | 北京大学 |
| code | varchar | 20 | NO | - | - | - | UK | 院校代码 | 教育部官方5位代码 | 10001 |
| province | varchar | 50 | NO | - | - | - | IDX | 所在省份 | 省级行政区全称 | 北京市 |
| city | varchar | 50 | NO | - | - | - | - | 所在城市 | 市级行政区全称 | 北京市 |
| level | varchar | 30 | NO | - | - | - | IDX | 院校层次 | 985/211/双一流/双非 | 985/211/双一流 |
| type | varchar | 30 | NO | - | - | - | - | 院校类型 | 综合/理工/师范/医药/财经等 | 综合 |
| belong | varchar | 50 | NO | - | - | - | - | 隶属单位 | 教育部/省属/市属/其他 | 教育部 |
| longitude | decimal | 10,6 | NO | - | - | - | - | 经度 | 高德地图查询，精确到6位小数 | 116.307628 |
| latitude | decimal | 10,6 | NO | - | - | - | - | 纬度 | 高德地图查询，精确到6位小数 | 39.985446 |
| website | varchar | 255 | NO | - | - | - | - | 院校官网 | 完整网址 | https://www.pku.edu.cn |
| logo | varchar | 255 | YES | NULL | - | - | - | 院校LOGO | 院校LOGO地址 | /logos/pku.png |
| status | tinyint | 1 | NO | 1 | - | - | IDX | 院校状态 | 1=启用（地图显示），0=禁用（不显示） | 1 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记，NULL表示未删除 | NULL |

**索引说明**:
- PK: PRIMARY KEY (id)
- UK: UNIQUE KEY (code) - 保证院校代码唯一
- IDX: INDEX (province, level, status, deleted_at) - 查询优化

### 2. subject_categories（学科门类表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 学科门类唯一ID | 自增主键 | 1 |
| code | varchar | 10 | NO | - | - | - | UK | 门类代码 | 官方代码（如01哲学） | 01 |
| name | varchar | 50 | NO | - | - | - | - | 学科门类全称 | 学科门类全称 | 哲学 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

### 3. disciplines（一级学科表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 一级学科唯一ID | 自增主键 | 1 |
| category_id | int | 11 | NO | - | - | ✅ | FK | 学科门类ID | 关联subject_categories.id | 1 |
| code | varchar | 20 | NO | - | - | - | UK | 一级学科代码 | 官方代码 | 0801 |
| name | varchar | 100 | NO | - | - | - | - | 一级学科全称 | 一级学科全称 | 力学 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

**外键约束**:
- FOREIGN KEY (category_id) REFERENCES subject_categories(id) ON DELETE CASCADE ON UPDATE CASCADE

### 4. majors（专业表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 专业唯一ID | 自增主键 | 1 |
| discipline_id | int | 11 | NO | - | - | ✅ | FK | 一级学科ID | 关联disciplines.id | 1 |
| code | varchar | 20 | NO | - | - | - | UK | 专业代码 | 官方6位代码 | 081200 |
| name | varchar | 100 | NO | - | - | - | IDX | 专业全称 | 官方完整名称 | 计算机科学与技术 |
| alias | varchar | 255 | YES | NULL | - | - | - | 专业别名 | 多个别名用逗号分隔 | 计科,计算机 |
| type | varchar | 10 | NO | - | - | - | - | 培养类型 | 学硕/专硕 | 学硕 |
| full_time | tinyint | 1 | NO | 1 | - | - | - | 是否全日制 | 1=全日制，0=非全日制 | 1 |
| remark | varchar | 255 | YES | NULL | - | - | - | 专业备注 | 特殊培养方向等 | 人工智能方向 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

**外键约束**:
- FOREIGN KEY (discipline_id) REFERENCES disciplines(id) ON DELETE CASCADE ON UPDATE CASCADE

### 5. school_majors（院校-专业关联表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 关联唯一ID | 自增主键 | 1 |
| school_id | int | 11 | NO | - | - | ✅ | FK,IDX | 院校ID | 关联schools.id | 1 |
| major_id | int | 11 | NO | - | - | ✅ | FK,IDX | 专业ID | 关联majors.id | 1 |
| status | tinyint | 1 | NO | 1 | - | - | - | 是否招生 | 1=招生，0=暂停招生 | 1 |
| remark | varchar | 255 | YES | NULL | - | - | - | 关联备注 | 招生院系、特殊要求 | 信息科学技术学院 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

**唯一约束**:
- UNIQUE KEY (school_id, major_id) - 防止重复关联
**外键约束**:
- FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE ON UPDATE CASCADE
- FOREIGN KEY (major_id) REFERENCES majors(id) ON DELETE CASCADE ON UPDATE CASCADE

### 6. admission_data（招生录取数据表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 数据唯一ID | 自增主键 | 1 |
| school_id | int | 11 | NO | - | - | ✅ | FK,IDX | 院校ID | 关联schools.id | 1 |
| major_id | int | 11 | NO | - | - | ✅ | FK,IDX | 专业ID | 关联majors.id | 1 |
| year | int | 4 | NO | - | - | - | IDX | 招生年份 | 2017-2024 | 2024 |
| plan_enroll | int | 11 | NO | 0 | - | - | - | 计划招生人数 | 非负整数 | 50 |
| actual_enroll | int | 11 | NO | 0 | - | - | - | 实际录取人数 | 非负整数（含推免） | 55 |
| recommended_count | int | 11 | NO | 0 | - | - | - | 推免人数 | 非负整数 | 10 |
| admission_ratio | decimal | 5,2 | YES | NULL | - | - | - | 报录比 | 保留2位小数 | 8.50 |
| retest_total_score | int | 11 | YES | NULL | - | - | - | 复试总分线 | 复试总分线 | 350 |
| single_subject_score | int | 11 | YES | NULL | - | - | - | 单科线 | 单科线 | 60 |
| average_admission_score | int | 11 | YES | NULL | - | - | - | 录取平均分 | 录取平均分 | 365 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

**唯一约束**:
- UNIQUE KEY (school_id, major_id, year) - 防止重复年份数据
**外键约束**:
- FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE ON UPDATE CASCADE
- FOREIGN KEY (major_id) REFERENCES majors(id) ON DELETE CASCADE ON UPDATE CASCADE

### 7. admins（管理员表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 管理员唯一ID | 自增主键 | 1 |
| username | varchar | 50 | NO | - | - | - | UK | 管理员账号 | 管理员登录账号 | admin |
| password | varchar | 255 | NO | - | - | - | - | 加密密码 | 使用BCrypt加密 | $2a$10$... |
| email | varchar | 100 | YES | NULL | - | - | UK | 邮箱 | 管理员邮箱 | admin@yanxiaomap.com |
| status | tinyint | 1 | NO | 1 | - | - | - | 状态 | 1=启用，0=禁用 | 1 |
| last_login_at | datetime | - | YES | NULL | - | - | - | 最后登录时间 | 最后登录时间 | 2026-04-17 10:00:00 |
| login_fail_count | int | 11 | NO | 0 | - | - | - | 登录失败次数 | 登录失败次数 | 0 |
| locked_until | datetime | - | YES | NULL | - | - | - | 锁定直到时间 | 账号锁定截止时间 | NULL |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

### 8. data_change_logs（数据修改记录表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 记录唯一ID | 自增主键 | 1 |
| table_name | varchar | 50 | NO | - | - | - | IDX | 表名 | 被修改的表名 | schools |
| record_id | int | 11 | NO | - | - | - | IDX | 记录ID | 被修改的记录ID | 1 |
| field_name | varchar | 50 | NO | - | - | - | - | 字段名 | 被修改的字段名 | name |
| old_value | text | - | YES | NULL | - | - | - | 修改前值 | 修改前的值 | 北京大学 |
| new_value | text | - | YES | NULL | - | - | - | 修改后值 | 修改后的值 | 北京大学（北京） |
| admin_id | int | 11 | YES | NULL | - | ✅ | IDX | 操作管理员ID | 关联admins.id | 1 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | IDX | 创建时间 | 操作时间 | 2026-04-17 10:00:00 |

### 9. system_configs（系统配置表）

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 外键 | 索引 | 中文名称 | 描述 | 示例 |
|--------|----------|------|--------|--------|------|------|------|----------|------|------|
| id | int | 11 | NO | - | ✅ | - | PK | 配置唯一ID | 自增主键 | 1 |
| config_key | varchar | 100 | NO | - | - | - | UK | 配置键 | 配置键名 | site_name |
| config_value | text | - | NO | - | - | - | - | 配置值 | 配置值 | 考研院校地图择校网站 |
| description | varchar | 255 | YES | NULL | - | - | - | 配置描述 | 配置描述 | 网站名称 |
| created_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 创建时间 | 记录创建时间 | 2026-04-17 10:00:00 |
| updated_at | datetime | - | NO | CURRENT_TIMESTAMP | - | - | - | 更新时间 | 记录更新时间 | 2026-04-17 10:00:00 |
| deleted_at | datetime | - | YES | NULL | - | - | IDX | 软删除时间 | 软删除标记 | NULL |

## 数据约束说明

### 1. 非空约束（NOT NULL）
- 所有业务关键字段均设置为NOT NULL，确保数据完整性
- 可选字段设置为NULL，如备注、LOGO等

### 2. 默认值约束（DEFAULT）
- 状态字段默认值为1（启用）
- 数值字段默认值为0
- 时间字段默认值为当前时间

### 3. 唯一约束（UNIQUE）
- 业务唯一字段设置唯一约束，防止数据重复
- 组合唯一约束用于防止重复关联

### 4. 外键约束（FOREIGN KEY）
- 所有关联关系均设置外键约束，保证数据一致性
- 使用级联删除和更新，简化数据维护

### 5. 检查约束（CHECK）
- 通过应用程序层实现数据校验
- 包括年份范围、数值范围、枚举值等

## 枚举值说明

### 1. schools.level（院校层次）
- 985/211/双一流
- 985/211
- 211/双一流
- 双一流
- 双非

### 2. schools.type（院校类型）
- 综合
- 理工
- 师范
- 医药
- 财经
- 政法
- 农林
- 民族
- 语言
- 艺术
- 体育
- 军事

### 3. schools.belong（隶属单位）
- 教育部
- 省属
- 市属
- 其他

### 4. majors.type（培养类型）
- 学硕
- 专硕

### 5. majors.full_time（是否全日制）
- 1: 全日制
- 0: 非全日制

### 6. school_majors.status（是否招生）
- 1: 招生
- 0: 暂停招生

### 7. admins.status（管理员状态）
- 1: 启用
- 0: 禁用

## 数据关系说明

### 1. 一对多关系
- subject_categories → disciplines
- disciplines → majors
- schools → admission_data (通过school_majors)

### 2. 多对多关系
- schools ↔ majors (通过school_majors)

### 3. 一对一关系
- admission_data → schools (通过school_id)
- admission_data → majors (通过major_id)

## 数据验证规则

### 1. 数值范围验证
- year: 2017-2024
- plan_enroll: ≥0
- actual_enroll: ≥0
- recommended_count: ≥0
- admission_ratio: ≥0
- retest_total_score: ≥0
- single_subject_score: ≥0
- average_admission_score: ≥0

### 2. 格式验证
- code: 数字或字母组合
- email: 邮箱格式
- website: URL格式
- longitude: -180~180，6位小数
- latitude: -90~90，6位小数

### 3. 长度验证
- 所有字符串字段长度符合定义
- 文本字段无长度限制

## 数据维护说明

### 1. 数据清理
- 定期清理deleted_at不为NULL的记录
- 归档历史数据到备份表

### 2. 数据备份
- 每日全量备份
- 实时增量备份
- 备份保留30天

### 3. 数据恢复
- 支持时间点恢复
- 支持单表恢复
- 恢复前验证数据完整性

## 附录

### 修订记录
| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|----------|--------|
| 1.0 | 2026-04-17 | 初始版本 | database Agent |

### 相关文档
- [数据库设计文档](design_document.md)
- [SQL建表脚本](schema.sql)
- [初始数据脚本](init_data.sql)
- [实体类目录](../database/entities/)

### 问题反馈
- 通过project_status.md文件通信
- 数据库相关问题联系database Agent
- 数据质量问题联系integration Agent