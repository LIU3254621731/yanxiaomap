#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据清洗配置模块
定义所有路径、文件映射和清洗规则配置
"""

from pathlib import Path
import os

# 项目根目录
PROJECT_ROOT = Path(r"c:\Users\32546\Desktop\yanxiaomap")

# 数据源目录
EXCEL_SOURCE_DIR = PROJECT_ROOT / "root" / "mydate17_24" / "excel"

# 输出目录
OUTPUT_DIR = PROJECT_ROOT / "database" / "cleaned_data"
OUTPUT_DIR.mkdir(exist_ok=True)

# 数据库表名
TABLE_NAMES = {
    'schools': 'schools',
    'subject_categories': 'subject_categories',
    'disciplines': 'disciplines',
    'majors': 'majors',
    'school_majors': 'school_majors',
    'admission_data': 'admission_data',
    'admins': 'admins',
    'data_change_logs': 'data_change_logs',
    'system_configs': 'system_configs'
}

# 关键Excel文件映射
EXCEL_FILE_MAPPING = {
    # 招生目录文件
    'admission_catalog_2024': [
        EXCEL_SOURCE_DIR / "报考" / "04-硕士研究生招生目录(2024).xlsx",
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "04-硕士研究生招生目录(2024).xlsx"
    ],
    'admission_catalog_2023': [
        EXCEL_SOURCE_DIR / "报考" / "23年.xlsx",
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "23年.xlsx"
    ],
    'admission_catalog_2022': [
        EXCEL_SOURCE_DIR / "报考" / "22年.xlsx",
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "22年.xlsx"
    ],
    # 复试分数线文件
    'retest_score_17_20': [
        EXCEL_SOURCE_DIR / "复试成绩" / "17-20研究生复试分数线.xlsx",
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "17-20研究生复试分数线.xlsx"
    ],
    'retest_score_20_22': [
        EXCEL_SOURCE_DIR / "复试成绩" / "20-22研究生复试分数线.xlsx",
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "20-22研究生复试分数线.xlsx"
    ],
    'retest_score_2023': [
        EXCEL_SOURCE_DIR / "复试成绩" / "2023研究生复试分数线.xlsx",
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "2023研究生复试分数线.xlsx"
    ],
    'retest_score_2024': [
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "2024年全国高等院校硕士研究生复试分数线.xlsx"
    ],
    # 招生计划文件
    'enrollment_plan_2024': [
        EXCEL_SOURCE_DIR / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "2024年全国高等院校硕士研究生的招生计划.xlsx"
    ]
}

# 工作表映射
SHEET_MAPPING = {
    'admission_catalog': ['Sheet1', 'Sheet2', 'Sheet3'],  # 招生目录文件中的工作表
    'retest_score': ['Sheet1'],  # 复试分数线文件中的工作表
    'enrollment_plan': ['Sheet1']  # 招生计划文件中的工作表
}

# 字段映射配置
FIELD_MAPPING = {
    # 院校字段映射
    'school': {
        'name': ['院校名称', '学校', '学校名称'],
        'code': ['院校代码'],
        'province': ['所在地', '学校省份', '所在城市'],
        'city': ['学校地址', '所在城市'],
        'type': ['院校类型', '学校属性'],
        'level': ['院校层级', '学校属性'],
        'is_985': ['985'],
        'is_211': ['211'],
        'is_double_first_class': ['双一流'],
        'is_self_line': ['自划线'],
        'zone': ['AB区'],
        'official_website': ['招生官网', '学校官网'],
        'admission_website': ['学校研究生官网'],
        'phone': ['招生电话', '学校电话'],
        'email': ['学校邮箱'],
        'address': ['学校地址'],
        'affiliation': ['隶属'],
        'master_programs_count': ['硕士点'],
        'doctoral_programs_count': ['博士点'],
        'national_key_disciplines': ['国家重点学科'],
        'national_key_laboratories': ['国家重点实验室', '重点实验室']
    },
    # 学科门类字段映射
    'subject_category': {
        'code': ['门类代码'],
        'name': ['门类名称']
    },
    # 一级学科字段映射
    'discipline': {
        'code': ['学科代码'],
        'name': ['学科领域']
    },
    # 专业字段映射
    'major': {
        'code': ['专业代码'],
        'name': ['专业名称'],
        'degree_type': ['学位性质', '专硕/学硕', '硕士类型'],
        'exam_method': ['考试方式']
    },
    # 院校-专业关联字段映射
    'school_major': {
        'department': ['招生院系', '院系所', '所属院系', '院系名称'],
        'research_direction': ['研究方向'],
        'study_mode': ['学习方式'],
        'advisor': ['指导教师', '指导老师'],
        'exam_subject1': ['业务课一', '专业课一'],
        'exam_subject2': ['业务课二', '专业课二'],
        'foreign_language': ['外语', '英语'],
        'political': ['政治', '政治综合']
    },
    # 招生数据字段映射
    'admission_data': {
        'year': ['年份'],
        'plan_enroll': ['拟招生人数', '招生人数', '拟招人数'],
        'actual_enroll': ['实际招生人数'],
        'retest_total_score': ['总分'],
        'retest_politics_score': ['政治', '政治__管综'],
        'retest_foreign_language_score': ['外语', '英语'],
        'retest_professional1_score': ['专业课一', '业务课_一'],
        'retest_professional2_score': ['专业课二', '业务课_二'],
        'notes': ['备注', '招生人数说明']
    }
}

# 清洗规则配置
CLEANING_RULES = {
    # 院校名称清洗规则
    'school_name': {
        'remove_patterns': [
            r'\([^)]*\)',  # 移除括号内容，如(10001)
            r'\[[^\]]*\]',  # 移除方括号内容
            r'\d+',  # 移除数字
            r'\s+',  # 规范化空格
        ],
        'replacements': [
            ('大学（', '大学'),
            ('学院（', '学院'),
            ('校区)', '校区'),
        ],
        'strip_chars': ' ()[]【】'
    },
    # 专业代码清洗规则
    'major_code': {
        'remove_patterns': [
            r'[^0-9]',  # 只保留数字
        ],
        'min_length': 6,
        'max_length': 6
    },
    # 专业名称清洗规则
    'major_name': {
        'extract_pattern': r'[（(]([^）)]+)[）)]',  # 提取括号内的内容
        'remove_patterns': [
            r'\([^)]*\)',  # 移除括号内容
            r'\d+',  # 移除数字
        ],
        'strip_chars': ' ()'
    },
    # 招生人数清洗规则
    'enrollment_number': {
        'extract_pattern': r'(\d+)',  # 提取数字
        'default_value': 0,
        'min_value': 0,
        'max_value': 10000
    },
    # 分数清洗规则
    'score': {
        'valid_range': (0, 500),
        'default_value': None,
        'invalid_values': ['-', '—', 'null', 'NULL', 'None', '无', '未公布']
    },
    # 标签解析规则
    'school_tags': {
        '985_keywords': ['985', '985工程', '985院校'],
        '211_keywords': ['211', '211工程', '211院校'],
        'double_first_class_keywords': ['双一流', '一流大学', '一流学科'],
        'self_line_keywords': ['自划线', '自主划线']
    }
}

# 数据质量检查配置
QUALITY_CHECKS = {
    'completeness_threshold': 0.7,  # 完整性阈值
    'consistency_threshold': 0.8,   # 一致性阈值
    'accuracy_threshold': 0.9,      # 准确性阈值
    
    # 必填字段检查
    'required_fields': {
        'schools': ['name', 'code'],
        'subject_categories': ['code', 'name'],
        'disciplines': ['code', 'name'],
        'majors': ['code', 'name'],
        'school_majors': ['school_id', 'major_id'],
        'admission_data': ['school_major_id', 'year']
    },
    
    # 数据类型检查
    'type_checks': {
        'schools': {
            'name': 'string',
            'code': 'string',
            'province': 'string',
            'is_985': 'boolean',
            'is_211': 'boolean',
            'master_programs_count': 'integer'
        },
        # 其他表类似...
    }
}

# 数据库连接配置（测试环境）
DATABASE_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'password',
    'database': 'yanxiaomap',
    'charset': 'utf8mb4'
}

# 日志配置
LOG_CONFIG = {
    'level': 'INFO',
    'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    'file_path': OUTPUT_DIR / 'cleaning.log'
}

# 输出文件配置
OUTPUT_FILES = {
    'schools': OUTPUT_DIR / 'schools.csv',
    'subject_categories': OUTPUT_DIR / 'subject_categories.csv',
    'disciplines': OUTPUT_DIR / 'disciplines.csv',
    'majors': OUTPUT_DIR / 'majors.csv',
    'school_majors': OUTPUT_DIR / 'school_majors.csv',
    'admission_data': OUTPUT_DIR / 'admission_data.csv',
    'cleaning_report': OUTPUT_DIR / 'cleaning_report.json',
    'quality_report': OUTPUT_DIR / 'quality_report.json'
}