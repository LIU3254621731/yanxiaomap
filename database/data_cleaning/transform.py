#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据转换模块
负责数据清洗、转换和规范化
"""

import pandas as pd
import numpy as np
from typing import Dict, List, Optional, Tuple, Union, Any
import logging
import re
from datetime import datetime

from .config import FIELD_MAPPING, CLEANING_RULES, TABLE_NAMES
from .utils import (
    clean_school_name, clean_major_code, clean_major_name,
    parse_enrollment_number, parse_score, parse_school_tags,
    extract_province_from_location, extract_city_from_address,
    normalize_degree_type, normalize_study_mode,
    clean_department_name, clean_research_direction,
    match_admission_data_batch
)

# 设置日志
logger = logging.getLogger(__name__)


class DataTransformer:
    """数据转换器"""
    
    def __init__(self):
        """初始化数据转换器"""
        self.processed_data = {}
        self.id_mappings = {
            'schools': {},      # 院校名称/代码 -> ID
            'subject_categories': {},  # 学科门类代码 -> ID
            'disciplines': {},  # 一级学科代码 -> ID
            'majors': {},       # 专业代码 -> ID
            'school_majors': {} # (学校ID, 专业ID) -> ID
        }
        
    def transform_all_data(self, raw_data: Dict[str, pd.DataFrame]) -> Dict[str, pd.DataFrame]:
        """
        转换所有类型的数据
        
        Args:
            raw_data: 原始数据字典
            
        Returns:
            转换后的数据字典
        """
        logger.info("开始转换所有数据...")
        
        transformed_data = {}
        
        # 转换顺序很重要：先转换基础表，再转换依赖表
        try:
            # 1. 转换学科门类（最基础）
            if 'subject_categories' in raw_data and not raw_data['subject_categories'].empty:
                transformed_data['subject_categories'] = self.transform_subject_categories(
                    raw_data['subject_categories']
                )
            
            # 2. 转换一级学科（依赖学科门类）
            if 'disciplines' in raw_data and not raw_data['disciplines'].empty:
                transformed_data['disciplines'] = self.transform_disciplines(
                    raw_data['disciplines']
                )
            
            # 3. 转换院校（独立）
            if 'schools' in raw_data and not raw_data['schools'].empty:
                transformed_data['schools'] = self.transform_schools(
                    raw_data['schools']
                )
            
            # 4. 转换专业（依赖一级学科）
            if 'majors' in raw_data and not raw_data['majors'].empty:
                transformed_data['majors'] = self.transform_majors(
                    raw_data['majors']
                )
            
            # 5. 转换院校-专业关联（依赖院校和专业）
            if 'school_majors' in raw_data and not raw_data['school_majors'].empty:
                transformed_data['school_majors'] = self.transform_school_majors(
                    raw_data['school_majors'],
                    transformed_data.get('schools', pd.DataFrame()),
                    transformed_data.get('majors', pd.DataFrame())
                )
            
            # 6. 转换招生数据（依赖院校-专业关联）
            if 'admission_data' in raw_data and not raw_data['admission_data'].empty:
                transformed_data['admission_data'] = self.transform_admission_data(
                    raw_data['admission_data'],
                    transformed_data.get('school_majors', pd.DataFrame()),
                    transformed_data.get('schools', pd.DataFrame()),
                    transformed_data.get('majors', pd.DataFrame())
                )
            
            logger.info("所有数据转换完成!")
            
            # 统计转换结果
            self._log_transformation_stats(transformed_data)
            
        except Exception as e:
            logger.error(f"数据转换失败: {e}")
            raise
        
        return transformed_data
    
    def _log_transformation_stats(self, transformed_data: Dict[str, pd.DataFrame]) -> None:
        """记录转换统计信息"""
        logger.info("转换结果统计:")
        for table_name, df in transformed_data.items():
            if not df.empty:
                logger.info(f"  {table_name}: {len(df)} 条记录")
                
                # 计算完整性
                completeness = df.notna().mean().mean() * 100
                logger.info(f"    数据完整性: {completeness:.1f}%")
    
    def transform_schools(self, raw_schools_df: pd.DataFrame) -> pd.DataFrame:
        """
        转换院校数据
        
        Args:
            raw_schools_df: 原始院校数据
            
        Returns:
            转换后的院校数据
        """
        logger.info("开始转换院校数据...")
        
        if raw_schools_df.empty:
            logger.warning("原始院校数据为空")
            return pd.DataFrame()
        
        # 创建副本
        schools_df = raw_schools_df.copy()
        
        # 1. 清洗院校名称
        if '院校名称' in schools_df.columns:
            schools_df['name'] = schools_df['院校名称'].apply(clean_school_name)
        else:
            logger.warning("院校名称列不存在")
            schools_df['name'] = ''
        
        # 2. 处理院校代码
        if '院校代码' in schools_df.columns:
            schools_df['code'] = schools_df['院校代码'].astype(str).str.strip()
        else:
            logger.warning("院校代码列不存在")
            schools_df['code'] = ''
        
        # 3. 提取省份
        if '所在地' in schools_df.columns:
            schools_df['province'] = schools_df['所在地'].apply(extract_province_from_location)
        else:
            schools_df['province'] = ''
        
        # 4. 提取城市
        if '学校地址' in schools_df.columns:
            schools_df['city'] = schools_df['学校地址'].apply(extract_city_from_address)
        else:
            schools_df['city'] = ''
        
        # 5. 解析标签字段
        tags_df = pd.DataFrame()
        
        # 解析985标签
        if '985' in schools_df.columns:
            tags_df['is_985'] = schools_df['985'].apply(
                lambda x: 1 if str(x).strip() in ['是', '1', 'true', 'True', 'Y', 'y'] else 0
            )
        else:
            tags_df['is_985'] = 0
        
        # 解析211标签
        if '211' in schools_df.columns:
            tags_df['is_211'] = schools_df['211'].apply(
                lambda x: 1 if str(x).strip() in ['是', '1', 'true', 'True', 'Y', 'y'] else 0
            )
        else:
            tags_df['is_211'] = 0
        
        # 解析双一流标签
        if '双一流' in schools_df.columns:
            tags_df['is_double_first_class'] = schools_df['双一流'].apply(
                lambda x: 1 if str(x).strip() in ['是', '1', 'true', 'True', 'Y', 'y'] else 0
            )
        else:
            tags_df['is_double_first_class'] = 0
        
        # 解析自划线标签
        if '自划线' in schools_df.columns:
            tags_df['is_self_line'] = schools_df['自划线'].apply(
                lambda x: 1 if str(x).strip() in ['是', '1', 'true', 'True', 'Y', 'y'] else 0
            )
        else:
            tags_df['is_self_line'] = 0
        
        # 6. 处理其他字段
        # 院校类型
        if '院校类型' in schools_df.columns:
            schools_df['type'] = schools_df['院校类型'].astype(str).str.strip()
        else:
            schools_df['type'] = ''
        
        # AB区
        if 'AB区' in schools_df.columns:
            schools_df['zone'] = schools_df['AB区'].astype(str).str.strip()
        else:
            schools_df['zone'] = ''
        
        # 官网
        if '招生官网' in schools_df.columns:
            schools_df['official_website'] = schools_df['招生官网'].astype(str).str.strip()
        else:
            schools_df['official_website'] = ''
        
        # 研究生官网
        if '学校研究生官网' in schools_df.columns:
            schools_df['admission_website'] = schools_df['学校研究生官网'].astype(str).str.strip()
        else:
            schools_df['admission_website'] = ''
        
        # 电话
        if '招生电话' in schools_df.columns:
            schools_df['phone'] = schools_df['招生电话'].astype(str).str.strip()
        else:
            schools_df['phone'] = ''
        
        # 邮箱
        if '学校邮箱' in schools_df.columns:
            schools_df['email'] = schools_df['学校邮箱'].astype(str).str.strip()
        else:
            schools_df['email'] = ''
        
        # 地址
        if '学校地址' in schools_df.columns:
            schools_df['address'] = schools_df['学校地址'].astype(str).str.strip()
        else:
            schools_df['address'] = ''
        
        # 隶属
        if '隶属' in schools_df.columns:
            schools_df['affiliation'] = schools_df['隶属'].astype(str).str.strip()
        else:
            schools_df['affiliation'] = ''
        
        # 数字字段处理
        numeric_fields = {
            'master_programs_count': '硕士点',
            'doctoral_programs_count': '博士点',
            'national_key_disciplines': '国家重点学科',
            'national_key_laboratories': '国家重点实验室'
        }
        
        for field, source in numeric_fields.items():
            if source in schools_df.columns:
                schools_df[field] = pd.to_numeric(schools_df[source], errors='coerce').fillna(0).astype(int)
            else:
                schools_df[field] = 0
        
        # 7. 确定院校层次（level）根据985/211/双一流标签
        def determine_school_level(row):
            is_985 = row.get('is_985', 0)
            is_211 = row.get('is_211', 0)
            is_double_first_class = row.get('is_double_first_class', 0)
            
            if is_985 == 1:
                return '985'
            elif is_211 == 1:
                return '211'
            elif is_double_first_class == 1:
                return '双一流'
            else:
                return '双非'
        
        # 创建tags_df的副本以便使用
        tags_df_copy = tags_df.copy()
        tags_df_copy['level'] = tags_df_copy.apply(determine_school_level, axis=1)
        
        # 8. 确定隶属单位（belong） - 根据affiliation字段映射
        def determine_belong(affiliation):
            if not affiliation or pd.isna(affiliation):
                return '其他'
            
            affiliation_str = str(affiliation).strip()
            if '教育部' in affiliation_str:
                return '教育部'
            elif '省属' in affiliation_str or '省教育厅' in affiliation_str:
                return '省属'
            elif '市属' in affiliation_str or '市教育局' in affiliation_str:
                return '市属'
            else:
                return '其他'
        
        if 'affiliation' in schools_df.columns:
            schools_df['belong'] = schools_df['affiliation'].apply(determine_belong)
        else:
            schools_df['belong'] = '其他'
        
        # 9. 确定官网（website） - 优先使用official_website，如果没有则用admission_website
        def determine_website(official, admission):
            if official and str(official).strip() and str(official).strip() != '':
                return str(official).strip()
            elif admission and str(admission).strip() and str(admission).strip() != '':
                return str(admission).strip()
            else:
                return ''
        
        if 'official_website' in schools_df.columns and 'admission_website' in schools_df.columns:
            schools_df['website'] = schools_df.apply(
                lambda row: determine_website(row['official_website'], row['admission_website']),
                axis=1
            )
        else:
            schools_df['website'] = ''
        
        # 10. 合并所有字段（匹配数据库schema）
        final_columns = [
            'name', 'code', 'province', 'city', 'type', 'belong', 'website'
        ]
        
        # 创建最终DataFrame
        final_df = pd.concat([schools_df[final_columns], tags_df_copy[['level']]], axis=1)
        
        # 11. 添加其他必要字段（schema中要求但当前数据没有的）
        final_df['logo'] = None  # 院校LOGO地址，后期扩展
        final_df['longitude'] = None  # 经度，需要高德地图API查询
        final_df['latitude'] = None   # 纬度，需要高德地图API查询
        final_df['status'] = 1        # 院校状态，1=启用（地图显示），0=禁用（不显示）
        
        # 12. 生成ID映射
        self._create_id_mapping(final_df, 'schools', 'code')
        
        # 13. 添加数据库字段
        final_df = self._add_database_fields(final_df, 'schools')
        
        # 14. 去重（基于代码）
        final_df = final_df.drop_duplicates(subset=['code'], keep='first')
        
        logger.info(f"院校数据转换完成: {len(final_df)} 条记录")
        logger.info(f"院校层次分布: 985-{sum(tags_df_copy['level'] == '985')}, "
                   f"211-{sum(tags_df_copy['level'] == '211')}, "
                   f"双一流-{sum(tags_df_copy['level'] == '双一流')}, "
                   f"双非-{sum(tags_df_copy['level'] == '双非')}")
        
        return final_df
    
    def transform_subject_categories(self, raw_subject_df: pd.DataFrame) -> pd.DataFrame:
        """
        转换学科门类数据
        
        Args:
            raw_subject_df: 原始学科门类数据
            
        Returns:
            转换后的学科门类数据
        """
        logger.info("开始转换学科门类数据...")
        
        if raw_subject_df.empty:
            logger.warning("原始学科门类数据为空")
            return pd.DataFrame()
        
        # 创建副本
        subject_df = raw_subject_df.copy()
        
        # 1. 清洗代码
        if '门类代码' in subject_df.columns:
            subject_df['code'] = subject_df['门类代码'].astype(str).str.strip()
        else:
            logger.warning("门类代码列不存在")
            subject_df['code'] = ''
        
        # 2. 清洗名称
        if '门类名称' in subject_df.columns:
            subject_df['name'] = subject_df['门类名称'].astype(str).str.strip()
        else:
            logger.warning("门类名称列不存在")
            subject_df['name'] = ''
        
        # 3. 选择所需列
        final_columns = ['code', 'name']
        final_df = subject_df[final_columns].copy()
        
        # 4. 去除空值
        final_df = final_df.dropna(subset=['code', 'name'])
        
        # 5. 去重
        final_df = final_df.drop_duplicates(subset=['code'], keep='first')
        
        # 6. 生成ID映射
        self._create_id_mapping(final_df, 'subject_categories', 'code')
        
        # 7. 添加数据库字段
        final_df = self._add_database_fields(final_df, 'subject_categories')
        
        logger.info(f"学科门类数据转换完成: {len(final_df)} 条记录")
        
        return final_df
    
    def transform_disciplines(self, raw_discipline_df: pd.DataFrame) -> pd.DataFrame:
        """
        转换一级学科数据
        
        Args:
            raw_discipline_df: 原始一级学科数据
            
        Returns:
            转换后的一级学科数据
        """
        logger.info("开始转换一级学科数据...")
        
        if raw_discipline_df.empty:
            logger.warning("原始一级学科数据为空")
            return pd.DataFrame()
        
        # 创建副本
        discipline_df = raw_discipline_df.copy()
        
        # 1. 清洗学科代码
        if '学科代码' in discipline_df.columns:
            discipline_df['code'] = discipline_df['学科代码'].astype(str).str.strip()
        else:
            logger.warning("学科代码列不存在")
            discipline_df['code'] = ''
        
        # 2. 清洗学科名称
        if '学科领域' in discipline_df.columns:
            discipline_df['name'] = discipline_df['学科领域'].astype(str).str.strip()
        else:
            logger.warning("学科领域列不存在")
            discipline_df['name'] = ''
        
        # 3. 获取学科门类ID
        if '门类代码' in discipline_df.columns:
            # 使用之前创建的ID映射
            discipline_df['subject_category_code'] = discipline_df['门类代码'].astype(str).str.strip()
            
            # 暂时存储代码，后续会替换为ID
            discipline_df['subject_category_id_temp'] = discipline_df['subject_category_code'].map(
                lambda x: self.id_mappings['subject_categories'].get(x, None)
            )
        else:
            logger.warning("门类代码列不存在")
            discipline_df['subject_category_id_temp'] = None
        
        # 4. 选择所需列
        final_columns = ['code', 'name', 'subject_category_id_temp']
        final_df = discipline_df[final_columns].copy()
        
        # 5. 重命名列
        final_df = final_df.rename(columns={'subject_category_id_temp': 'subject_category_id'})
        
        # 6. 去除空值
        final_df = final_df.dropna(subset=['code', 'name'])
        
        # 7. 去重
        final_df = final_df.drop_duplicates(subset=['code'], keep='first')
        
        # 8. 生成ID映射
        self._create_id_mapping(final_df, 'disciplines', 'code')
        
        # 9. 添加数据库字段
        final_df = self._add_database_fields(final_df, 'disciplines')
        
        logger.info(f"一级学科数据转换完成: {len(final_df)} 条记录")
        
        return final_df
    
    def transform_majors(self, raw_majors_df: pd.DataFrame) -> pd.DataFrame:
        """
        转换专业数据
        
        Args:
            raw_majors_df: 原始专业数据
            
        Returns:
            转换后的专业数据
        """
        logger.info("开始转换专业数据...")
        
        if raw_majors_df.empty:
            logger.warning("原始专业数据为空")
            return pd.DataFrame()
        
        # 创建副本
        majors_df = raw_majors_df.copy()
        
        # 1. 清洗专业代码
        if '专业代码' in majors_df.columns:
            majors_df['code'] = majors_df['专业代码'].apply(clean_major_code)
        else:
            logger.warning("专业代码列不存在")
            majors_df['code'] = ''
        
        # 2. 清洗专业名称
        if '专业名称' in majors_df.columns:
            majors_df['name'] = majors_df['专业名称'].apply(clean_major_name)
        else:
            logger.warning("专业名称列不存在")
            majors_df['name'] = ''
        
        # 3. 确定培养类型（type）- 根据学位性质
        if '学位性质' in majors_df.columns:
            majors_df['type'] = majors_df['学位性质'].apply(normalize_degree_type)
        else:
            majors_df['type'] = '学硕'  # 默认值
        
        # 4. 确定是否全日制（full_time）- 默认全日制
        # 可以从考试方式或其他字段推断，但目前默认设为1（全日制）
        majors_df['full_time'] = 1
        
        # 5. 别名字段（alias）- 可以为空，用于搜索
        majors_df['alias'] = None
        
        # 6. 备注字段（remark）- 可以为空
        majors_df['remark'] = None
        
        # 5. 获取一级学科ID
        if '学科代码' in majors_df.columns:
            majors_df['discipline_code'] = majors_df['学科代码'].astype(str).str.strip()
            
            # 映射到一级学科ID
            majors_df['discipline_id_temp'] = majors_df['discipline_code'].map(
                lambda x: self.id_mappings['disciplines'].get(x, None)
            )
        else:
            majors_df['discipline_id_temp'] = None
        
        # 6. 选择所需列（匹配数据库schema）
        final_columns = ['code', 'name', 'discipline_id_temp', 'type', 'full_time', 'alias', 'remark']
        final_df = majors_df[final_columns].copy()
        
        # 7. 重命名列
        final_df = final_df.rename(columns={'discipline_id_temp': 'discipline_id'})
        
        # 8. 去除空值
        final_df = final_df.dropna(subset=['code', 'name'])
        
        # 9. 去重
        final_df = final_df.drop_duplicates(subset=['code'], keep='first')
        
        # 10. 生成ID映射
        self._create_id_mapping(final_df, 'majors', 'code')
        
        # 11. 添加数据库字段
        final_df = self._add_database_fields(final_df, 'majors')
        
        logger.info(f"专业数据转换完成: {len(final_df)} 条记录")
        
        return final_df
    
    def transform_school_majors(self, raw_school_majors_df: pd.DataFrame,
                               schools_df: pd.DataFrame, majors_df: pd.DataFrame) -> pd.DataFrame:
        """
        转换院校-专业关联数据
        
        Args:
            raw_school_majors_df: 原始院校-专业关联数据
            schools_df: 转换后的院校数据
            majors_df: 转换后的专业数据
            
        Returns:
            转换后的院校-专业关联数据
        """
        logger.info("开始转换院校-专业关联数据...")
        
        if raw_school_majors_df.empty:
            logger.warning("原始院校-专业关联数据为空")
            return pd.DataFrame()
        
        if schools_df.empty or majors_df.empty:
            logger.warning("院校数据或专业数据为空，无法转换关联数据")
            return pd.DataFrame()
        
        # 创建副本
        school_majors_df = raw_school_majors_df.copy()
        
        # 1. 获取院校ID
        if '院校代码' in school_majors_df.columns:
            school_majors_df['school_code'] = school_majors_df['院校代码'].astype(str).str.strip()
            school_majors_df['school_id_temp'] = school_majors_df['school_code'].map(
                lambda x: self.id_mappings['schools'].get(x, None)
            )
        else:
            logger.warning("院校代码列不存在")
            school_majors_df['school_id_temp'] = None
        
        # 2. 获取专业ID
        if '专业代码' in school_majors_df.columns:
            school_majors_df['major_code'] = school_majors_df['专业代码'].apply(clean_major_code)
            school_majors_df['major_id_temp'] = school_majors_df['major_code'].map(
                lambda x: self.id_mappings['majors'].get(x, None)
            )
        else:
            logger.warning("专业代码列不存在")
            school_majors_df['major_id_temp'] = None
        
        # 3. 清洗其他字段
        # 院系名称
        if '招生院系' in school_majors_df.columns:
            school_majors_df['department'] = school_majors_df['招生院系'].apply(clean_department_name)
        else:
            school_majors_df['department'] = ''
        
        # 研究方向
        if '研究方向' in school_majors_df.columns:
            school_majors_df['research_direction'] = school_majors_df['研究方向'].apply(clean_research_direction)
        else:
            school_majors_df['research_direction'] = ''
        
        # 学习方式
        if '学习方式' in school_majors_df.columns:
            school_majors_df['study_mode'] = school_majors_df['学习方式'].apply(normalize_study_mode)
        else:
            school_majors_df['study_mode'] = '全日制'
        
        # 指导教师
        if '指导教师' in school_majors_df.columns:
            school_majors_df['advisor'] = school_majors_df['指导教师'].astype(str).str.strip()
        else:
            school_majors_df['advisor'] = ''
        
        # 考试科目
        exam_subjects = ['业务课一', '业务课二', '外语', '政治']
        for subject in exam_subjects:
            if subject in school_majors_df.columns:
                col_name = 'exam_subject1' if subject == '业务课一' else \
                          'exam_subject2' if subject == '业务课二' else \
                          'foreign_language' if subject == '外语' else \
                          'political'
                school_majors_df[col_name] = school_majors_df[subject].astype(str).str.strip()
            else:
                col_name = 'exam_subject1' if subject == '业务课一' else \
                          'exam_subject2' if subject == '业务课二' else \
                          'foreign_language' if subject == '外语' else \
                          'political'
                school_majors_df[col_name] = ''
        
        # 4. 选择所需列（匹配数据库schema）
        final_columns = ['school_id_temp', 'major_id_temp']
        final_df = school_majors_df[final_columns].copy()
        
        # 5. 重命名列并添加status字段
        final_df = final_df.rename(columns={
            'school_id_temp': 'school_id',
            'major_id_temp': 'major_id'
        })
        
        # 添加status字段（1=招生，0=暂停招生）
        final_df['status'] = 1
        
        # 6. 去除无效关联（缺少院校ID或专业ID）
        final_df = final_df.dropna(subset=['school_id', 'major_id'])
        
        # 7. 去重（相同的院校-专业组合）
        final_df = final_df.drop_duplicates(subset=['school_id', 'major_id'], keep='first')
        
        # 8. 添加院校名称和专业名称（用于匹配）
        # 创建反向映射：ID -> 代码
        school_id_to_code = {v: k for k, v in self.id_mappings['schools'].items()}
        major_id_to_code = {v: k for k, v in self.id_mappings['majors'].items()}
        
        # 创建代码到名称的映射
        school_code_to_name = {}
        if not schools_df.empty and 'code' in schools_df.columns and 'name' in schools_df.columns:
            for _, row in schools_df.iterrows():
                school_code_to_name[row['code']] = row['name']
        
        major_code_to_name = {}
        if not majors_df.empty and 'code' in majors_df.columns and 'name' in majors_df.columns:
            for _, row in majors_df.iterrows():
                major_code_to_name[row['code']] = row['name']
        
        # 添加名称列
        school_names = []
        major_names = []
        
        for _, row in final_df.iterrows():
            school_id = row['school_id']
            major_id = row['major_id']
            
            # 获取院校名称
            school_name = ''
            if school_id in school_id_to_code:
                school_code = school_id_to_code[school_id]
                school_name = school_code_to_name.get(school_code, '')
            school_names.append(school_name)
            
            # 获取专业名称
            major_name = ''
            if major_id in major_id_to_code:
                major_code = major_id_to_code[major_id]
                major_name = major_code_to_name.get(major_code, '')
            major_names.append(major_name)
        
        final_df['school_name'] = school_names
        final_df['major_name'] = major_names
        
        # 9. 生成复合键ID映射
        self._create_composite_id_mapping(final_df, 'school_majors', ['school_id', 'major_id'])
        
        # 10. 添加数据库字段
        final_df = self._add_database_fields(final_df, 'school_majors')
        
        logger.info(f"院校-专业关联数据转换完成: {len(final_df)} 条记录")
        
        return final_df
    
    def transform_admission_data(self, raw_admission_df: pd.DataFrame,
                                school_majors_df: pd.DataFrame,
                                schools_df: pd.DataFrame = None,
                                majors_df: pd.DataFrame = None) -> pd.DataFrame:
        """
        转换招生数据
        
        Args:
            raw_admission_df: 原始招生数据
            school_majors_df: 转换后的院校-专业关联数据
            schools_df: 转换后的院校数据（用于匹配）
            majors_df: 转换后的专业数据（用于匹配）
            
        Returns:
            转换后的招生数据
        """
        logger.info("开始转换招生数据...")
        
        if raw_admission_df.empty:
            logger.warning("原始招生数据为空")
            return pd.DataFrame()
        
        if school_majors_df.empty:
            logger.warning("院校-专业关联数据为空，无法转换招生数据")
            return pd.DataFrame()
        
        # 创建副本
        admission_df = raw_admission_df.copy()
        
        # 1. 提取年份
        if '年份' in admission_df.columns:
            admission_df['year'] = pd.to_numeric(admission_df['年份'], errors='coerce')
        else:
            # 尝试从文件名或其他字段推断年份
            admission_df['year'] = 2024  # 默认值
        
        # 2. 解析招生人数
        if '拟招生人数' in admission_df.columns:
            admission_df['plan_enroll'] = admission_df['拟招生人数'].apply(parse_enrollment_number)
        else:
            admission_df['plan_enroll'] = 0
        
        if '实际招生人数' in admission_df.columns:
            admission_df['actual_enroll'] = admission_df['实际招生人数'].apply(parse_enrollment_number)
        else:
            admission_df['actual_enroll'] = 0
        
        # 3. 解析分数
        # 根据数据库schema，admission_data表只有retest_total_score和single_subject_score
        # 原始数据可能有多个分数字段，需要映射
        
        # 解析总分
        if '总分' in admission_df.columns:
            admission_df['retest_total_score'] = admission_df['总分'].apply(parse_score)
        else:
            admission_df['retest_total_score'] = None
        
        # 解析单科分数 - 选择最低的单科分数
        single_subject_scores = []
        
        # 可能的单科分数字段
        single_score_fields = ['政治', '外语', '专业课一', '专业课二']
        
        for idx, row in admission_df.iterrows():
            min_score = None
            
            # 检查每个单科字段
            for field in single_score_fields:
                if field in admission_df.columns and not pd.isna(row[field]):
                    score = parse_score(row[field])
                    if score is not None:
                        if min_score is None or score < min_score:
                            min_score = score
            
            single_subject_scores.append(min_score)
        
        admission_df['single_subject_score'] = single_subject_scores
        

        
        # 5. 缺失字段（目前Excel中不存在）
        admission_df['recommended_count'] = None  # 推免人数
        admission_df['admission_ratio'] = None  # 报录比
        admission_df['average_admission_score'] = None  # 录取平均分
        
        # 6. 获取院校-专业关联ID - 使用批量匹配函数
        logger.info("开始匹配院校-专业关联ID...")
        admission_df = match_admission_data_batch(
            admission_df,
            school_majors_df,
            schools_df,
            majors_df
        )
        
        # 7. 选择所需列（匹配数据库schema）
        final_columns = [
            'school_id', 'major_id', 'year', 'plan_enroll', 'actual_enroll',
            'recommended_count', 'admission_ratio',
            'retest_total_score', 'single_subject_score',
            'average_admission_score'
        ]
        
        # 确保所有列都存在
        for col in final_columns:
            if col not in admission_df.columns:
                admission_df[col] = None
        
        final_df = admission_df[final_columns].copy()
        
        # 8. 去除完全空值的行
        final_df = final_df.dropna(how='all')
        
        # 9. 添加数据库字段
        final_df = self._add_database_fields(final_df, 'admission_data')
        
        logger.info(f"招生数据转换完成: {len(final_df)} 条记录")
        
        return final_df
    
    def _create_id_mapping(self, df: pd.DataFrame, table_name: str, key_column: str) -> None:
        """
        创建ID映射
        
        Args:
            df: 数据DataFrame
            table_name: 表名
            key_column: 关键列（用于生成映射）
        """
        if df.empty:
            return
        
        # 生成ID（从1开始）
        ids = list(range(1, len(df) + 1))
        
        # 创建映射
        for i, key in enumerate(df[key_column]):
            self.id_mappings[table_name][key] = ids[i]
        
        logger.debug(f"为 {table_name} 创建了 {len(df)} 个ID映射")
    
    def _create_composite_id_mapping(self, df: pd.DataFrame, table_name: str, key_columns: List[str]) -> None:
        """
        创建复合键ID映射
        
        Args:
            df: 数据DataFrame
            table_name: 表名
            key_columns: 关键列列表
        """
        if df.empty:
            return
        
        # 生成ID（从1开始）
        ids = list(range(1, len(df) + 1))
        
        # 创建复合键
        for i, (idx, row) in enumerate(df.iterrows()):
            composite_key = tuple(row[col] for col in key_columns)
            self.id_mappings[table_name][composite_key] = ids[i]
        
        logger.debug(f"为 {table_name} 创建了 {len(df)} 个复合键ID映射")
    
    def _add_database_fields(self, df: pd.DataFrame, table_name: str) -> pd.DataFrame:
        """
        添加数据库字段（如created_at, updated_at等）
        
        Args:
            df: 数据DataFrame
            table_name: 表名
            
        Returns:
            添加了数据库字段的DataFrame
        """
        # 添加时间戳
        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        df['created_at'] = current_time
        df['updated_at'] = current_time
        
        # 根据表名添加特定字段
        if table_name == 'schools':
            # 添加经度和纬度（默认为空）
            if 'longitude' not in df.columns:
                df['longitude'] = None
            if 'latitude' not in df.columns:
                df['latitude'] = None
            
            # 添加状态字段
            df['status'] = 1  # 1表示正常
            
        elif table_name in ['subject_categories', 'disciplines', 'majors']:
            # 添加排序字段
            df['sort_order'] = range(1, len(df) + 1)
            
        elif table_name == 'admission_data':
            # 添加数据来源标记
            df['data_source'] = 'excel'
            
        # 所有表都添加软删除字段
        df['deleted_at'] = None
        
        return df


def test_transformation():
    """测试转换功能"""
    import sys
    from .extract import ExcelExtractor
    from .config import LOG_CONFIG
    from .utils import setup_logging
    
    # 设置日志
    setup_logging(LOG_CONFIG['file_path'])
    
    logger.info("开始测试数据转换...")
    
    try:
        # 1. 提取数据
        extractor = ExcelExtractor()
        raw_data = extractor.extract_all_data()
        
        if not any(not df.empty for df in raw_data.values()):
            logger.error("未提取到任何数据，无法进行转换测试")
            sys.exit(1)
        
        # 2. 转换数据
        transformer = DataTransformer()
        transformed_data = transformer.transform_all_data(raw_data)
        
        if transformed_data:
            logger.info("数据转换测试成功!")
            
            # 打印示例数据
            for table_name, df in transformed_data.items():
                if not df.empty:
                    logger.info(f"\n{table_name} 转换后示例数据 (前2行):")
                    logger.info(df.head(2).to_string(index=False))
                    
                    # 打印列名
                    logger.info(f"列: {list(df.columns)}")
        
        else:
            logger.warning("数据转换测试失败，未生成任何转换后数据")
            
    except Exception as e:
        logger.error(f"数据转换测试失败: {e}")
        import traceback
        logger.error(traceback.format_exc())
        sys.exit(1)


if __name__ == "__main__":
    test_transformation()