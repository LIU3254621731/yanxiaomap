#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简化数据清洗测试脚本
只处理院校、学科门类和一级学科数据
"""

import pandas as pd
import logging
from pathlib import Path
import sys

# 添加data_cleaning到路径
sys.path.insert(0, str(Path(__file__).parent))

from data_cleaning.extract import ExcelExtractor
from data_cleaning.transform import DataTransformer
from data_cleaning.load import DataLoader

def setup_logging():
    """设置日志"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler()
        ]
    )

def main():
    """主函数"""
    setup_logging()
    logger = logging.getLogger(__name__)
    
    logger.info("开始简化数据清洗测试...")
    
    try:
        # 1. 提取数据
        logger.info("步骤1: 提取数据...")
        extractor = ExcelExtractor()
        
        # 只提取基础数据
        logger.info("提取院校数据...")
        schools_raw = extractor.extract_schools_data()
        logger.info(f"提取到 {len(schools_raw)} 条院校记录")
        
        logger.info("提取学科门类数据...")
        subject_categories_raw = extractor.extract_subject_categories_data()
        logger.info(f"提取到 {len(subject_categories_raw)} 条学科门类记录")
        
        logger.info("提取一级学科数据...")
        disciplines_raw = extractor.extract_disciplines_data()
        logger.info(f"提取到 {len(disciplines_raw)} 条一级学科记录")
        
        # 2. 转换数据
        logger.info("步骤2: 转换数据...")
        transformer = DataTransformer()
        
        # 转换院校数据
        if not schools_raw.empty:
            schools_transformed = transformer.transform_schools(schools_raw)
            logger.info(f"转换后院校数据: {len(schools_transformed)} 条记录")
        else:
            schools_transformed = pd.DataFrame()
            logger.warning("院校数据为空")
        
        # 转换学科门类数据
        if not subject_categories_raw.empty:
            subject_categories_transformed = transformer.transform_subject_categories(subject_categories_raw)
            logger.info(f"转换后学科门类数据: {len(subject_categories_transformed)} 条记录")
        else:
            subject_categories_transformed = pd.DataFrame()
            logger.warning("学科门类数据为空")
        
        # 转换一级学科数据
        if not disciplines_raw.empty:
            disciplines_transformed = transformer.transform_disciplines(disciplines_raw)
            logger.info(f"转换后一级学科数据: {len(disciplines_transformed)} 条记录")
        else:
            disciplines_transformed = pd.DataFrame()
            logger.warning("一级学科数据为空")
        
        # 3. 保存数据
        logger.info("步骤3: 保存数据...")
        
        # 创建输出目录
        output_dir = Path(__file__).parent / "cleaned_data_test"
        output_dir.mkdir(exist_ok=True)
        
        # 保存院校数据
        if not schools_transformed.empty:
            schools_file = output_dir / "schools.csv"
            schools_transformed.to_csv(schools_file, index=False, encoding='utf-8-sig')
            logger.info(f"院校数据保存到: {schools_file}")
            
            # 打印示例
            logger.info("院校数据示例 (前3行):")
            print(schools_transformed.head(3).to_string(index=False))
        
        # 保存学科门类数据
        if not subject_categories_transformed.empty:
            subject_categories_file = output_dir / "subject_categories.csv"
            subject_categories_transformed.to_csv(subject_categories_file, index=False, encoding='utf-8-sig')
            logger.info(f"学科门类数据保存到: {subject_categories_file}")
            
            # 打印示例
            logger.info("学科门类数据示例 (前5行):")
            print(subject_categories_transformed.head(5).to_string(index=False))
        
        # 保存一级学科数据
        if not disciplines_transformed.empty:
            disciplines_file = output_dir / "disciplines.csv"
            disciplines_transformed.to_csv(disciplines_file, index=False, encoding='utf-8-sig')
            logger.info(f"一级学科数据保存到: {disciplines_file}")
            
            # 打印示例
            logger.info("一级学科数据示例 (前5行):")
            print(disciplines_transformed.head(5).to_string(index=False))
        
        # 4. 生成SQL脚本
        logger.info("步骤4: 生成SQL脚本...")
        
        sql_dir = output_dir / "sql_scripts"
        sql_dir.mkdir(exist_ok=True)
        
        # 生成院校SQL
        if not schools_transformed.empty:
            schools_sql_file = sql_dir / "insert_schools.sql"
            with open(schools_sql_file, 'w', encoding='utf-8') as f:
                f.write("-- 院校数据插入脚本\n")
                f.write(f"-- 生成时间: {pd.Timestamp.now()}\n")
                f.write(f"-- 记录数: {len(schools_transformed)}\n\n")
                
                for _, row in schools_transformed.iterrows():
                    values = []
                    for col in schools_transformed.columns:
                        val = row[col]
                        if pd.isna(val):
                            values.append('NULL')
                        elif isinstance(val, (int, float)):
                            values.append(str(val))
                        elif isinstance(val, bool):
                            values.append('1' if val else '0')
                        else:
                            escaped = str(val).replace("'", "''")
                            values.append(f"'{escaped}'")
                    
                    insert_sql = f"INSERT INTO schools ({', '.join(schools_transformed.columns)}) VALUES ({', '.join(values)});\n"
                    f.write(insert_sql)
            
            logger.info(f"院校SQL脚本生成: {schools_sql_file}")
        
        logger.info("简化数据清洗测试完成!")
        
        # 汇总
        logger.info("\n" + "="*60)
        logger.info("测试结果汇总:")
        logger.info(f"院校数据: {len(schools_transformed) if not schools_transformed.empty else 0} 条记录")
        logger.info(f"学科门类数据: {len(subject_categories_transformed) if not subject_categories_transformed.empty else 0} 条记录")
        logger.info(f"一级学科数据: {len(disciplines_transformed) if not disciplines_transformed.empty else 0} 条记录")
        logger.info(f"输出目录: {output_dir}")
        logger.info("="*60)
        
    except Exception as e:
        logger.error(f"简化数据清洗测试失败: {e}")
        import traceback
        logger.error(traceback.format_exc())
        sys.exit(1)

if __name__ == "__main__":
    main()