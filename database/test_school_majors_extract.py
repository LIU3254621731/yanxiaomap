#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试院校-专业关联数据提取
"""

import sys
import os
import pandas as pd

# 添加父目录到路径，以便导入data_cleaning模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from data_cleaning.extract import ExcelExtractor
from data_cleaning.config import LOG_CONFIG
from data_cleaning.utils import setup_logging

def test_school_majors_extraction():
    """测试院校-专业关联数据提取"""
    # 设置日志
    log_file = LOG_CONFIG['file_path']
    if isinstance(log_file, str):
        log_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), log_file)
    setup_logging(log_file)
    
    import logging
    logger = logging.getLogger(__name__)
    
    logger.info("开始测试院校-专业关联数据提取...")
    
    try:
        # 创建提取器
        extractor = ExcelExtractor()
        
        # 提取院校-专业关联数据
        logger.info("提取院校-专业关联数据...")
        school_majors_df = extractor.extract_school_majors_data()
        
        if school_majors_df.empty:
            logger.warning("未提取到院校-专业关联数据")
            print("未提取到院校-专业关联数据")
            
            # 检查提取过程中发生了什么
            logger.info("检查所有文件数据...")
            all_data = extractor.extract_all_files(parallel=False)
            print(f"招生目录文件数量: {len(all_data.get('admission_catalog', []))}")
            
            if all_data.get('admission_catalog'):
                for i, df in enumerate(all_data['admission_catalog']):
                    print(f"招生目录文件 {i}: {df.shape} 行 x {df.shape[1]} 列")
                    print(f"列名: {list(df.columns)}")
                    if '院校名称' in df.columns and '专业名称' in df.columns:
                        print(f"文件 {i} 包含院校名称和专业名称列")
                        sample = df[['院校名称', '专业名称']].head(3)
                        print(f"示例数据:\n{sample}")
                        break
        else:
            logger.info(f"成功提取院校-专业关联数据: {school_majors_df.shape[0]} 条记录")
            print(f"成功提取院校-专业关联数据: {school_majors_df.shape[0]} 条记录")
            print(f"数据列: {list(school_majors_df.columns)}")
            
            # 显示前几行数据
            print("\n前5行数据:")
            print(school_majors_df.head().to_string(index=False))
            
            # 检查关键列
            required_cols = ['院校名称', '专业名称', '院校代码', '专业代码']
            for col in required_cols:
                if col in school_majors_df.columns:
                    non_null = school_majors_df[col].notna().sum()
                    print(f"{col}: {non_null}/{len(school_majors_df)} 非空值")
                else:
                    print(f"{col}: 列不存在")
    
    except Exception as e:
        logger.error(f"测试失败: {e}")
        import traceback
        logger.error(traceback.format_exc())
        print(f"测试失败: {e}")
        sys.exit(1)
    
    logger.info("院校-专业关联数据提取测试完成!")

if __name__ == "__main__":
    test_school_majors_extraction()