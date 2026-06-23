#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据提取模块
负责从Excel文件中提取原始数据
"""

import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Union, Any
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
import warnings

from .config import EXCEL_FILE_MAPPING, SHEET_MAPPING
from .utils import setup_logging

# 设置日志
logger = logging.getLogger(__name__)

# 忽略pandas警告
warnings.filterwarnings('ignore', category=UserWarning, module='openpyxl')

class ExcelExtractor:
    """Excel数据提取器"""
    
    def __init__(self, excel_dir: Optional[Path] = None):
        """
        初始化Excel提取器
        
        Args:
            excel_dir: Excel目录路径，如果为None则使用配置中的路径
        """
        self.excel_dir = excel_dir or Path(r"c:\Users\32546\Desktop\yanxiaomap\root\mydate17_24\excel")
        self.data_cache = {}  # 数据缓存
        
    def extract_all_files(self, parallel: bool = True) -> Dict[str, List[pd.DataFrame]]:
        """
        提取所有Excel文件数据
        
        Args:
            parallel: 是否并行处理
            
        Returns:
            按文件类型分组的数据字典
        """
        logger.info("开始提取所有Excel文件数据...")
        
        all_data = {
            'admission_catalog': [],  # 招生目录
            'retest_score': [],       # 复试分数线
            'enrollment_plan': []     # 招生计划
        }
        
        try:
            if parallel:
                all_data = self._extract_parallel()
            else:
                all_data = self._extract_sequential()
            
            logger.info(f"数据提取完成: 招生目录{len(all_data['admission_catalog'])}个文件, "
                       f"复试分数线{len(all_data['retest_score'])}个文件, "
                       f"招生计划{len(all_data['enrollment_plan'])}个文件")
            
        except Exception as e:
            logger.error(f"提取Excel文件数据失败: {e}")
            raise
        
        return all_data
    
    def _extract_sequential(self) -> Dict[str, List[pd.DataFrame]]:
        """顺序提取数据"""
        all_data = {
            'admission_catalog': [],
            'retest_score': [],
            'enrollment_plan': []
        }
        
        # 提取招生目录文件
        for file_type in ['admission_catalog_2024', 'admission_catalog_2023', 'admission_catalog_2022']:
            if file_type in EXCEL_FILE_MAPPING:
                for file_path in EXCEL_FILE_MAPPING[file_type]:
                    if file_path.exists():
                        df = self.extract_file(file_path, 'admission_catalog')
                        if df is not None:
                            all_data['admission_catalog'].append(df)
                    else:
                        logger.warning(f"文件不存在: {file_path}")
        
        # 提取复试分数线文件
        for file_type in ['retest_score_17_20', 'retest_score_20_22', 'retest_score_2023', 'retest_score_2024']:
            if file_type in EXCEL_FILE_MAPPING:
                for file_path in EXCEL_FILE_MAPPING[file_type]:
                    if file_path.exists():
                        df = self.extract_file(file_path, 'retest_score')
                        if df is not None:
                            all_data['retest_score'].append(df)
                    else:
                        logger.warning(f"文件不存在: {file_path}")
        
        # 提取招生计划文件
        if 'enrollment_plan_2024' in EXCEL_FILE_MAPPING:
            for file_path in EXCEL_FILE_MAPPING['enrollment_plan_2024']:
                if file_path.exists():
                    df = self.extract_file(file_path, 'enrollment_plan')
                    if df is not None:
                        all_data['enrollment_plan'].append(df)
                else:
                    logger.warning(f"文件不存在: {file_path}")
        
        return all_data
    
    def _extract_parallel(self) -> Dict[str, List[pd.DataFrame]]:
        """并行提取数据"""
        all_data = {
            'admission_catalog': [],
            'retest_score': [],
            'enrollment_plan': []
        }
        
        # 收集所有需要提取的文件
        extract_tasks = []
        
        # 招生目录文件
        for file_type in ['admission_catalog_2024', 'admission_catalog_2023', 'admission_catalog_2022']:
            if file_type in EXCEL_FILE_MAPPING:
                for file_path in EXCEL_FILE_MAPPING[file_type]:
                    extract_tasks.append((file_path, 'admission_catalog'))
        
        # 复试分数线文件
        for file_type in ['retest_score_17_20', 'retest_score_20_22', 'retest_score_2023', 'retest_score_2024']:
            if file_type in EXCEL_FILE_MAPPING:
                for file_path in EXCEL_FILE_MAPPING[file_type]:
                    extract_tasks.append((file_path, 'retest_score'))
        
        # 招生计划文件
        if 'enrollment_plan_2024' in EXCEL_FILE_MAPPING:
            for file_path in EXCEL_FILE_MAPPING['enrollment_plan_2024']:
                extract_tasks.append((file_path, 'enrollment_plan'))
        
        # 并行执行提取任务
        with ThreadPoolExecutor(max_workers=4) as executor:
            future_to_task = {
                executor.submit(self.extract_file, file_path, file_category): (file_path, file_category)
                for file_path, file_category in extract_tasks
                if file_path.exists()
            }
            
            for future in as_completed(future_to_task):
                file_path, file_category = future_to_task[future]
                try:
                    df = future.result()
                    if df is not None:
                        all_data[file_category].append(df)
                except Exception as e:
                    logger.error(f"提取文件 {file_path} 失败: {e}")
        
        return all_data
    
    def extract_file(self, file_path: Path, file_category: str) -> Optional[pd.DataFrame]:
        """
        提取单个Excel文件
        
        Args:
            file_path: Excel文件路径
            file_category: 文件类别
            
        Returns:
            提取的数据DataFrame，失败返回None
        """
        # 检查文件是否在缓存中
        cache_key = f"{file_path}_{file_category}"
        if cache_key in self.data_cache:
            logger.debug(f"从缓存加载: {file_path}")
            return self.data_cache[cache_key]
        
        try:
            logger.info(f"正在提取文件: {file_path}")
            
            # 获取工作表名称
            sheet_names = self._get_sheet_names(file_path, file_category)
            
            # 读取所有工作表数据
            all_sheets_data = []
            for sheet_name in sheet_names:
                try:
                    df = pd.read_excel(
                        file_path,
                        sheet_name=sheet_name,
                        dtype=str,  # 所有列作为字符串读取，避免类型推断问题
                        engine='openpyxl'
                    )
                    
                    # 添加工作表信息和文件信息
                    df['_sheet_name'] = sheet_name
                    df['_source_file'] = file_path.name
                    df['_file_category'] = file_category
                    
                    all_sheets_data.append(df)
                    
                    logger.debug(f"读取工作表 {sheet_name} 成功: {df.shape[0]} 行, {df.shape[1]} 列")
                    
                except Exception as e:
                    logger.warning(f"读取工作表 {sheet_name} 失败: {e}")
                    continue
            
            if not all_sheets_data:
                logger.warning(f"文件 {file_path} 没有可读的工作表")
                return None
            
            # 合并所有工作表数据
            combined_df = pd.concat(all_sheets_data, ignore_index=True, sort=False)
            
            # 缓存数据
            self.data_cache[cache_key] = combined_df
            
            logger.info(f"文件提取完成: {file_path.name} - {combined_df.shape[0]} 行, {combined_df.shape[1]} 列")
            
            return combined_df
            
        except Exception as e:
            logger.error(f"提取文件 {file_path} 失败: {e}")
            return None
    
    def _get_sheet_names(self, file_path: Path, file_category: str) -> List[str]:
        """
        获取文件的工作表名称
        
        Args:
            file_path: Excel文件路径
            file_category: 文件类别
            
        Returns:
            工作表名称列表
        """
        try:
            # 尝试从Excel文件获取所有工作表
            xl = pd.ExcelFile(file_path, engine='openpyxl')
            all_sheets = xl.sheet_names
            
            # 根据文件类别过滤工作表
            if file_category in SHEET_MAPPING:
                expected_sheets = SHEET_MAPPING[file_category]
                # 返回匹配的工作表，如果都不匹配则返回所有工作表
                matched_sheets = [s for s in all_sheets if s in expected_sheets]
                if matched_sheets:
                    return matched_sheets
            
            # 如果没有匹配的，返回所有工作表
            return all_sheets
            
        except Exception as e:
            logger.warning(f"获取工作表名称失败 {file_path}: {e}")
            # 返回默认工作表
            return ['Sheet1']
    
    def extract_schools_data(self) -> pd.DataFrame:
        """
        提取院校数据
        
        Returns:
            院校数据DataFrame
        """
        logger.info("开始提取院校数据...")
        
        all_data = self.extract_all_files(parallel=True)
        
        # 合并所有招生目录数据
        admission_dfs = all_data['admission_catalog']
        if not admission_dfs:
            logger.warning("没有招生目录数据")
            return pd.DataFrame()
        
        combined_df = pd.concat(admission_dfs, ignore_index=True, sort=False)
        
        # 提取院校相关列
        school_columns = [
            '院校名称', '院校代码', '所在地', '院校类型', '院校层级',
            '985', '211', '双一流', '自划线', 'AB区',
            '招生官网', '学校研究生官网', '招生电话', '学校电话',
            '学校邮箱', '学校地址', '隶属',
            '硕士点', '博士点', '国家重点学科', '国家重点实验室',
            '_source_file', '_file_category'
        ]
        
        # 选择存在的列
        available_columns = [col for col in school_columns if col in combined_df.columns]
        missing_columns = set(school_columns) - set(available_columns)
        
        if missing_columns:
            logger.warning(f"院校数据缺失列: {missing_columns}")
        
        schools_df = combined_df[available_columns].copy()
        
        # 去重
        schools_df = schools_df.drop_duplicates(subset=['院校代码', '院校名称'], keep='first')
        
        logger.info(f"提取院校数据完成: {schools_df.shape[0]} 条记录")
        
        return schools_df
    
    def extract_subject_categories_data(self) -> pd.DataFrame:
        """
        提取学科门类数据
        
        Returns:
            学科门类数据DataFrame
        """
        logger.info("开始提取学科门类数据...")
        
        all_data = self.extract_all_files(parallel=False)
        
        # 合并所有招生目录数据
        admission_dfs = all_data['admission_catalog']
        if not admission_dfs:
            logger.warning("没有招生目录数据")
            return pd.DataFrame()
        
        combined_df = pd.concat(admission_dfs, ignore_index=True, sort=False)
        
        # 提取学科门类相关列
        subject_columns = ['门类代码', '门类名称']
        
        # 选择存在的列
        available_columns = [col for col in subject_columns if col in combined_df.columns]
        missing_columns = set(subject_columns) - set(available_columns)
        
        if missing_columns:
            logger.warning(f"学科门类数据缺失列: {missing_columns}")
            return pd.DataFrame()
        
        subject_df = combined_df[available_columns].copy()
        
        # 去重
        subject_df = subject_df.drop_duplicates(subset=['门类代码'], keep='first')
        
        # 去除空值
        subject_df = subject_df.dropna(subset=['门类代码', '门类名称'])
        
        logger.info(f"提取学科门类数据完成: {subject_df.shape[0]} 条记录")
        
        return subject_df
    
    def extract_disciplines_data(self) -> pd.DataFrame:
        """
        提取一级学科数据
        
        Returns:
            一级学科数据DataFrame
        """
        logger.info("开始提取一级学科数据...")
        
        all_data = self.extract_all_files(parallel=False)
        
        # 合并所有招生目录数据
        admission_dfs = all_data['admission_catalog']
        if not admission_dfs:
            logger.warning("没有招生目录数据")
            return pd.DataFrame()
        
        combined_df = pd.concat(admission_dfs, ignore_index=True, sort=False)
        
        # 提取一级学科相关列
        discipline_columns = ['学科代码', '学科领域', '门类代码']
        
        # 选择存在的列
        available_columns = [col for col in discipline_columns if col in combined_df.columns]
        missing_columns = set(discipline_columns) - set(available_columns)
        
        if missing_columns:
            logger.warning(f"一级学科数据缺失列: {missing_columns}")
            return pd.DataFrame()
        
        discipline_df = combined_df[available_columns].copy()
        
        # 去重
        discipline_df = discipline_df.drop_duplicates(subset=['学科代码'], keep='first')
        
        # 去除空值
        discipline_df = discipline_df.dropna(subset=['学科代码', '学科领域'])
        
        logger.info(f"提取一级学科数据完成: {discipline_df.shape[0]} 条记录")
        
        return discipline_df
    
    def extract_majors_data(self) -> pd.DataFrame:
        """
        提取专业数据
        
        Returns:
            专业数据DataFrame
        """
        logger.info("开始提取专业数据...")
        
        all_data = self.extract_all_files(parallel=False)
        
        # 合并所有招生目录数据
        admission_dfs = all_data['admission_catalog']
        if not admission_dfs:
            logger.warning("没有招生目录数据")
            return pd.DataFrame()
        
        combined_df = pd.concat(admission_dfs, ignore_index=True, sort=False)
        
        # 提取专业相关列
        major_columns = ['专业代码', '专业名称', '学位性质', '考试方式', '学科代码']
        
        # 选择存在的列
        available_columns = [col for col in major_columns if col in combined_df.columns]
        missing_columns = set(major_columns) - set(available_columns)
        
        if missing_columns:
            logger.warning(f"专业数据缺失列: {missing_columns}")
        
        majors_df = combined_df[available_columns].copy()
        
        # 检查是否有足够的列
        if majors_df.empty:
            logger.warning("专业数据为空")
            return majors_df
        
        # 处理缺失的专业代码列
        if '专业代码' not in majors_df.columns and '专业名称' in majors_df.columns:
            logger.info("专业代码列缺失，尝试从专业名称中提取...")
            # 从专业名称中提取专业代码
            from .utils import extract_major_code_from_name
            majors_df['extracted_code'] = majors_df['专业名称'].apply(extract_major_code_from_name)
            
            # 将提取的代码作为专业代码
            majors_df['专业代码'] = majors_df['extracted_code']
            majors_df = majors_df.drop(columns=['extracted_code'])
            
            # 记录提取结果
            extracted_count = majors_df['专业代码'].notna().sum()
            logger.info(f"从专业名称中提取了 {extracted_count} 个专业代码")
        
        # 确定去重列 - 优先使用专业代码，如果没有则使用专业名称
        if '专业代码' in majors_df.columns and majors_df['专业代码'].notna().any():
            subset_cols = ['专业代码']
            logger.info("使用专业代码进行去重")
        elif '专业名称' in majors_df.columns:
            subset_cols = ['专业名称']
            logger.warning("使用专业名称进行去重（专业代码列缺失或为空）")
        else:
            logger.error("专业数据既无专业代码也无专业名称，无法处理")
            return pd.DataFrame()
        
        # 去重
        majors_df = majors_df.drop_duplicates(subset=subset_cols, keep='first')
        
        # 去除空值
        required_cols = [col for col in ['专业代码', '专业名称'] if col in majors_df.columns]
        if required_cols:
            # 如果专业代码列存在但大部分为空，允许专业名称为空
            if '专业代码' in required_cols and majors_df['专业代码'].isna().mean() > 0.5:
                logger.warning("专业代码大部分为空，只要求专业名称不为空")
                required_cols = ['专业名称'] if '专业名称' in majors_df.columns else []
            
            if required_cols:
                majors_df = majors_df.dropna(subset=required_cols)
        
        logger.info(f"提取专业数据完成: {majors_df.shape[0]} 条记录")
        
        return majors_df
    
    def extract_school_majors_data(self) -> pd.DataFrame:
        """
        提取院校-专业关联数据
        
        Returns:
            院校-专业关联数据DataFrame
        """
        logger.info("开始提取院校-专业关联数据...")
        
        all_data = self.extract_all_files(parallel=False)
        
        # 合并所有招生目录数据
        admission_dfs = all_data['admission_catalog']
        if not admission_dfs:
            logger.warning("没有招生目录数据")
            return pd.DataFrame()
        
        combined_df = pd.concat(admission_dfs, ignore_index=True, sort=False)
        
        # 提取院校-专业关联相关列
        school_major_columns = [
            '院校代码', '院校名称', '专业代码', '专业名称',
            '招生院系', '研究方向', '学习方式',
            '指导教师', '业务课一', '业务课二', '外语', '政治'
        ]
        
        # 选择存在的列
        available_columns = [col for col in school_major_columns if col in combined_df.columns]
        missing_columns = set(school_major_columns) - set(available_columns)
        
        if missing_columns:
            logger.warning(f"院校-专业关联数据缺失列: {missing_columns}")
        
        school_majors_df = combined_df[available_columns].copy()
        
        # 去除空值（至少需要院校和专业信息）
        # 尝试使用代码列，如果不存在则使用名称列
        required_code_cols = ['院校代码', '专业代码']
        required_name_cols = ['院校名称', '专业名称']
        
        if all(col in school_majors_df.columns for col in required_code_cols):
            # 有代码列，使用代码列去重
            school_majors_df = school_majors_df.dropna(subset=required_code_cols)
            logger.info("使用院校代码和专业代码进行去重")
        elif all(col in school_majors_df.columns for col in required_name_cols):
            # 只有名称列，使用名称列去重
            school_majors_df = school_majors_df.dropna(subset=required_name_cols)
            logger.info("使用院校名称和专业名称进行去重")
            
            # 尝试从专业名称中提取专业代码
            if '专业代码' not in school_majors_df.columns and '专业名称' in school_majors_df.columns:
                logger.info("专业代码列缺失，尝试从专业名称中提取...")
                from .utils import extract_major_code_from_name
                school_majors_df['extracted_major_code'] = school_majors_df['专业名称'].apply(extract_major_code_from_name)
                school_majors_df['专业代码'] = school_majors_df['extracted_major_code']
                # 删除临时列
                school_majors_df = school_majors_df.drop(columns=['extracted_major_code'], errors='ignore')
        else:
            logger.warning("缺失必要的院校代码/名称或专业代码/名称列")
            return pd.DataFrame()
        
        logger.info(f"提取院校-专业关联数据完成: {school_majors_df.shape[0]} 条记录")
        
        return school_majors_df
    
    def extract_admission_data(self) -> pd.DataFrame:
        """
        提取招生数据
        
        Returns:
            招生数据DataFrame
        """
        logger.info("开始提取招生数据...")
        
        all_data = self.extract_all_files(parallel=False)
        
        # 合并所有复试分数线和招生目录数据
        retest_dfs = all_data['retest_score']
        admission_dfs = all_data['admission_catalog']
        
        if not retest_dfs and not admission_dfs:
            logger.warning("没有复试分数线和招生目录数据")
            return pd.DataFrame()
        
        # 处理复试分数线数据
        admission_data_list = []
        
        # 从复试分数线文件提取数据
        for retest_df in retest_dfs:
            # 提取招生相关列
            admission_columns = [
                '院校名称', '专业名称', '年份',
                '总分', '政治', '外语', '专业课一', '专业课二',
                '拟招生人数', '实际招生人数', '备注'
            ]
            
            # 选择存在的列
            available_columns = [col for col in admission_columns if col in retest_df.columns]
            if available_columns:
                admission_df = retest_df[available_columns].copy()
                admission_data_list.append(admission_df)
        
        # 从招生目录文件提取数据
        for admission_df in admission_dfs:
            # 提取招生相关列
            admission_columns = [
                '院校名称', '专业名称',
                '拟招生人数', '备注'
            ]
            
            # 选择存在的列
            available_columns = [col for col in admission_columns if col in admission_df.columns]
            if available_columns:
                admission_df = admission_df[available_columns].copy()
                admission_data_list.append(admission_df)
        
        if not admission_data_list:
            logger.warning("没有可用的招生数据")
            return pd.DataFrame()
        
        # 合并所有招生数据
        combined_admission_df = pd.concat(admission_data_list, ignore_index=True, sort=False)
        
        # 去除完全空值的行
        combined_admission_df = combined_admission_df.dropna(how='all')
        
        logger.info(f"提取招生数据完成: {combined_admission_df.shape[0]} 条记录")
        
        return combined_admission_df
    
    def extract_all_data(self) -> Dict[str, pd.DataFrame]:
        """
        提取所有类型的数据
        
        Returns:
            按类型分组的数据字典
        """
        logger.info("开始提取所有类型的数据...")
        
        all_data = {}
        
        # 提取各种类型的数据
        all_data['schools'] = self.extract_schools_data()
        all_data['subject_categories'] = self.extract_subject_categories_data()
        all_data['disciplines'] = self.extract_disciplines_data()
        all_data['majors'] = self.extract_majors_data()
        all_data['school_majors'] = self.extract_school_majors_data()
        all_data['admission_data'] = self.extract_admission_data()
        
        # 统计
        total_records = sum(len(df) for df in all_data.values())
        logger.info(f"所有数据提取完成，共 {total_records} 条记录")
        
        # 打印各表记录数
        for table_name, df in all_data.items():
            logger.info(f"  {table_name}: {len(df)} 条记录")
        
        return all_data


def test_extraction():
    """测试提取功能"""
    import sys
    
    # 设置日志
    from .config import LOG_CONFIG
    setup_logging(LOG_CONFIG['file_path'])
    
    logger.info("开始测试数据提取...")
    
    try:
        extractor = ExcelExtractor()
        
        # 测试提取所有数据
        all_data = extractor.extract_all_data()
        
        if all_data:
            logger.info("数据提取测试成功!")
            
            # 打印示例数据
            for table_name, df in all_data.items():
                if not df.empty:
                    logger.info(f"\n{table_name} 示例数据 (前3行):")
                    logger.info(df.head(3).to_string(index=False))
        
        else:
            logger.warning("数据提取测试失败，未提取到任何数据")
            
    except Exception as e:
        logger.error(f"数据提取测试失败: {e}")
        import traceback
        logger.error(traceback.format_exc())
        sys.exit(1)


if __name__ == "__main__":
    test_extraction()