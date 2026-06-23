-- 考研院校地图择校网站数据库建表脚本
-- 数据库版本: MySQL 8.0
-- 字符集: UTF8MB4
-- 存储引擎: InnoDB
-- 创建时间: 2026-04-17

-- 设置数据库字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
DROP DATABASE IF EXISTS `yanxiaomap`;
CREATE DATABASE IF NOT EXISTS `yanxiaomap` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yanxiaomap`;

-- 院校表（schools）
CREATE TABLE `schools` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '院校唯一ID',
  `name` varchar(100) NOT NULL COMMENT '院校全称，官方完整名称',
  `code` varchar(20) NOT NULL COMMENT '教育部官方5位院校代码，唯一',
  `province` varchar(50) NOT NULL COMMENT '所在省份，省级行政区全称',
  `city` varchar(50) NOT NULL COMMENT '所在城市，市级行政区全称',
  `level` varchar(30) NOT NULL COMMENT '院校层次，枚举：985/211/双一流/双非',
  `type` varchar(30) NOT NULL COMMENT '院校类型，枚举：综合/理工/师范/医药/财经等',
  `belong` varchar(50) NOT NULL COMMENT '隶属单位，枚举：教育部/省属/市属/其他',
  `enrollment_unit` varchar(100) DEFAULT NULL COMMENT '招生单位，如研究生院/招生办公室',
  `address` varchar(255) DEFAULT NULL COMMENT '院校详细地址',
  `introduction` text COMMENT '院校简介',
  `established_year` int(4) DEFAULT NULL COMMENT '建校年份',
  `longitude` decimal(10,6) NOT NULL COMMENT '经度，高德地图查询，精确到6位小数',
  `latitude` decimal(10,6) NOT NULL COMMENT '纬度，高德地图查询，精确到6位小数',
  `website` varchar(255) NOT NULL COMMENT '院校官网，完整网址',
  `logo` varchar(255) DEFAULT NULL COMMENT '院校LOGO地址，后期扩展',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '院校状态，1=启用（地图显示），0=禁用（不显示）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schools_code` (`code`),
  KEY `idx_schools_province` (`province`),
  KEY `idx_schools_level` (`level`),
  KEY `idx_schools_status` (`status`),
  KEY `idx_schools_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校表';

-- 学科门类表（subject_categories）
CREATE TABLE `subject_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '学科门类唯一ID',
  `code` varchar(10) NOT NULL COMMENT '门类官方代码（如01哲学）',
  `name` varchar(50) NOT NULL COMMENT '学科门类全称',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_categories_code` (`code`),
  KEY `idx_subject_categories_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科门类表';

-- 一级学科表（disciplines）
CREATE TABLE `disciplines` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '一级学科唯一ID',
  `category_id` int(11) NOT NULL COMMENT '所属学科门类ID，外键关联subject_categories表id',
  `code` varchar(20) NOT NULL COMMENT '一级学科官方代码',
  `name` varchar(100) NOT NULL COMMENT '一级学科全称',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_disciplines_code` (`code`),
  KEY `fk_disciplines_category_id` (`category_id`),
  KEY `idx_disciplines_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_disciplines_category_id` FOREIGN KEY (`category_id`) REFERENCES `subject_categories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='一级学科表';

-- 专业表（majors）
CREATE TABLE `majors` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '专业唯一ID',
  `discipline_id` int(11) NOT NULL COMMENT '所属一级学科ID，外键关联disciplines表id',
  `category_id` int(11) DEFAULT NULL COMMENT '所属学科门类ID，外键关联subject_categories表id',
  `code` varchar(20) NOT NULL COMMENT '专业官方6位代码，唯一',
  `name` varchar(100) NOT NULL COMMENT '专业全称，官方完整名称',
  `alias` varchar(255) DEFAULT NULL COMMENT '专业别名，多个别名用逗号分隔，用于搜索',
  `type` varchar(10) NOT NULL COMMENT '培养类型，枚举：学硕/专硕',
  `degree_type` varchar(50) DEFAULT NULL COMMENT '学位类型，如工学硕士/理学硕士',
  `full_time` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否全日制，1=全日制，0=非全日制',
  `duration` int(2) DEFAULT NULL COMMENT '学制（年）',
  `description` text COMMENT '专业描述',
  `training_objective` text COMMENT '培养目标',
  `main_courses` text COMMENT '主要课程',
  `employment_direction` text COMMENT '就业方向',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '专业状态，1=启用，0=禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '专业备注，特殊培养方向等',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_majors_code` (`code`),
  KEY `fk_majors_discipline_id` (`discipline_id`),
  KEY `idx_majors_name` (`name`),
  KEY `idx_majors_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_majors_discipline_id` FOREIGN KEY (`discipline_id`) REFERENCES `disciplines` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- 院校-专业关联表（school_majors）
CREATE TABLE `school_majors` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '关联唯一ID',
  `school_id` int(11) NOT NULL COMMENT '院校ID，外键关联schools表id',
  `major_id` int(11) NOT NULL COMMENT '专业ID，外键关联majors表id',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否招生，1=招生，0=暂停招生',
  `remark` varchar(255) DEFAULT NULL COMMENT '关联备注，如招生院系、特殊要求',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_majors_school_major` (`school_id`, `major_id`),
  KEY `fk_school_majors_school_id` (`school_id`),
  KEY `fk_school_majors_major_id` (`major_id`),
  KEY `idx_school_majors_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_school_majors_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_school_majors_major_id` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校-专业关联表';

-- 招生录取数据表（admission_data）
CREATE TABLE `admission_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '数据唯一ID',
  `school_id` int(11) NOT NULL COMMENT '院校ID，外键关联schools表id',
  `major_id` int(11) NOT NULL COMMENT '专业ID，外键关联majors表id',
  `year` int(4) NOT NULL COMMENT '招生年份，2017-2024',
  `plan_enroll` int(11) NOT NULL DEFAULT '0' COMMENT '计划招生人数，非负整数',
  `actual_enroll` int(11) NOT NULL DEFAULT '0' COMMENT '实际录取人数（含推免），非负整数',
  `recommended_count` int(11) NOT NULL DEFAULT '0' COMMENT '推免人数，非负整数',
  `admission_ratio` decimal(5,2) DEFAULT NULL COMMENT '报录比，保留2位小数',
  `retest_total_score` int(11) DEFAULT NULL COMMENT '复试总分线',
  `single_subject_score` int(11) DEFAULT NULL COMMENT '单科线',
  `average_admission_score` int(11) DEFAULT NULL COMMENT '录取平均分',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admission_data_school_major_year` (`school_id`, `major_id`, `year`),
  KEY `fk_admission_data_school_id` (`school_id`),
  KEY `fk_admission_data_major_id` (`major_id`),
  KEY `idx_admission_data_year` (`year`),
  KEY `idx_admission_data_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_admission_data_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_admission_data_major_id` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招生录取数据表';

-- 管理员表（admins）【根据需求文档，后端管理端需要管理员登录】
CREATE TABLE `admins` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '管理员唯一ID',
  `username` varchar(50) NOT NULL COMMENT '管理员账号',
  `password` varchar(255) NOT NULL COMMENT '加密密码，使用BCrypt加密',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态，1=启用，0=禁用',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `login_fail_count` int(11) NOT NULL DEFAULT '0' COMMENT '登录失败次数',
  `locked_until` datetime DEFAULT NULL COMMENT '锁定直到时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admins_username` (`username`),
  UNIQUE KEY `uk_admins_email` (`email`),
  KEY `idx_admins_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 数据修改记录表（data_change_logs）【根据需求文档，数据修改记录留存】
CREATE TABLE `data_change_logs` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '记录唯一ID',
  `table_name` varchar(50) NOT NULL COMMENT '表名',
  `record_id` int(11) NOT NULL COMMENT '记录ID',
  `field_name` varchar(50) NOT NULL COMMENT '字段名',
  `old_value` text COMMENT '修改前值',
  `new_value` text COMMENT '修改后值',
  `admin_id` int(11) DEFAULT NULL COMMENT '操作管理员ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_data_change_logs_table_record` (`table_name`, `record_id`),
  KEY `idx_data_change_logs_admin_id` (`admin_id`),
  KEY `idx_data_change_logs_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据修改记录表';

-- 系统配置表（system_configs）【根据需求文档，系统基础配置】
CREATE TABLE `system_configs` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '配置唯一ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text NOT NULL COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_configs_key` (`config_key`),
  KEY `idx_system_configs_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 用户表（user）
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
  `username` varchar(50) NOT NULL COMMENT '用户名，唯一',
  `password` varchar(255) NOT NULL COMMENT '密码，BCrypt加密存储',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT '用户角色：admin-管理员，user-普通用户',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '用户状态：0-禁用，1-启用',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `register_ip` varchar(50) DEFAULT NULL COMMENT '注册IP',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `register_time` datetime DEFAULT NULL COMMENT '注册时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 初始化管理员账号（默认账号admin，密码admin123）
INSERT INTO `admins` (`username`, `password`, `email`, `status`) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVpUi.', 'admin@yanxiaomap.com', 1);

-- 初始化系统配置
INSERT INTO `system_configs` (`config_key`, `config_value`, `description`) VALUES
('site_name', '考研院校地图择校网站', '网站名称'),
('site_logo', '', '网站LOGO地址'),
('icp_record', '', 'ICP备案号'),
('security_record', '', '公安网安备案号'),
('gaode_map_key', '', '高德地图API密钥'),
('login_captcha_enabled', '1', '登录图形验证码是否启用'),
('login_lock_threshold', '3', '登录失败锁定阈值'),
('login_lock_duration', '3600', '登录锁定持续时间（秒）');

-- 注释：实际密码需要在前端使用BCrypt加密后存储，这里仅为示例