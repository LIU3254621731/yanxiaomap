#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据加载模块
负责将清洗后的数据保存到文件和数据库
"""

import pandas as pd
import numpy as np
from typing import Dict, List, Optional, Tuple, Union, Any
import logging
import json
from pathlib import Path
from datetime import datetime

# 可选数据库依赖
try:
    import mysql.connector
    from mysql.connector import Error
    MYSQL_AVAILABLE = True
except ImportError:
    mysql = None
    Error = type('Error', (Exception,), {})
    MYSQL_AVAILABLE = False
    logging.getLogger(__name__).warning("mysql-connector-python未安装，数据库功能将不可用")

try:
    import sqlalchemy
    from sqlalchemy import create_engine, text
    SQLALCHEMY_AVAILABLE = True
except ImportError:
    sqlalchemy = None
    create_engine = text = None
    SQLALCHEMY_AVAILABLE = False
    logging.getLogger(__name__).warning("SQLAlchemy未安装，数据库功能将受限")

from .config import OUTPUT_FILES, DATABASE_CONFIG, TABLE_NAMES
from .utils import save_to_csv, calculate_data_quality_metrics

# 设置日志
logger = logging.getLogger(__name__)


class DataLoader:
    """数据加载器"""
    
    def __init__(self, output_dir: Optional[Path] = None):
        """
        初始化数据加载器
        
        Args:
            output_dir: 输出目录路径
        """
        self.output_dir = output_dir or OUTPUT_FILES['schools'].parent
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
    def save_to_files(self, transformed_data: Dict[str, pd.DataFrame]) -> Dict[str, Path]:
        """
        将转换后的数据保存到文件
        
        Args:
            transformed_data: 转换后的数据字典
            
        Returns:
            保存的文件路径字典
        """
        logger.info("开始保存数据到文件...")
        
        saved_files = {}
        
        try:
            for table_name, df in transformed_data.items():
                if df.empty:
                    logger.warning(f"{table_name} 数据为空，跳过保存")
                    continue
                
                # 获取输出文件路径
                if table_name in OUTPUT_FILES:
                    file_path = OUTPUT_FILES[table_name]
                else:
                    file_path = self.output_dir / f"{table_name}.csv"
                
                # 保存到CSV
                if save_to_csv(df, file_path):
                    saved_files[table_name] = file_path
                    logger.info(f"  {table_name}: 保存到 {file_path} ({len(df)} 条记录)")
                else:
                    logger.error(f"  {table_name}: 保存失败")
            
            logger.info(f"数据保存完成，共保存 {len(saved_files)} 个文件")
            
            # 生成数据质量报告
            self._generate_quality_report(transformed_data)
            
            # 生成清洗报告
            self._generate_cleaning_report(transformed_data, saved_files)
            
        except Exception as e:
            logger.error(f"保存数据到文件失败: {e}")
            raise
        
        return saved_files
    
    def _generate_quality_report(self, transformed_data: Dict[str, pd.DataFrame]) -> None:
        """
        生成数据质量报告
        
        Args:
            transformed_data: 转换后的数据字典
        """
        logger.info("生成数据质量报告...")
        
        quality_report = {
            'generated_at': datetime.now().isoformat(),
            'tables': {},
            'summary': {
                'total_tables': 0,
                'total_records': 0,
                'avg_completeness': 0.0,
                'avg_consistency': 0.0,
                'avg_accuracy': 0.0
            }
        }
        
        # 各表的质量指标
        table_metrics = []
        
        for table_name, df in transformed_data.items():
            if df.empty:
                continue
            
            # 计算质量指标（简化实现）
            required_columns = list(df.columns)  # 这里应该根据业务规则定义必填列
            metrics = calculate_data_quality_metrics(df, required_columns)
            
            quality_report['tables'][table_name] = {
                'record_count': len(df),
                'column_count': len(df.columns),
                'completeness': metrics['completeness'],
                'consistency': metrics['consistency'],
                'accuracy': metrics['accuracy'],
                'duplicate_rows': metrics['duplicate_rows']
            }
            
            table_metrics.append(metrics)
        
        # 计算总体指标
        if table_metrics:
            quality_report['summary']['total_tables'] = len(quality_report['tables'])
            quality_report['summary']['total_records'] = sum(
                info['record_count'] for info in quality_report['tables'].values()
            )
            quality_report['summary']['avg_completeness'] = np.mean(
                [m['completeness'] for m in table_metrics]
            )
            quality_report['summary']['avg_consistency'] = np.mean(
                [m['consistency'] for m in table_metrics]
            )
            quality_report['summary']['avg_accuracy'] = np.mean(
                [m['accuracy'] for m in table_metrics]
            )
        
        # 保存质量报告
        report_path = OUTPUT_FILES['quality_report']
        try:
            with open(report_path, 'w', encoding='utf-8') as f:
                json.dump(quality_report, f, ensure_ascii=False, indent=2)
            logger.info(f"数据质量报告已保存到: {report_path}")
        except Exception as e:
            logger.error(f"保存数据质量报告失败: {e}")
    
    def _generate_cleaning_report(self, transformed_data: Dict[str, pd.DataFrame],
                                 saved_files: Dict[str, Path]) -> None:
        """
        生成清洗报告
        
        Args:
            transformed_data: 转换后的数据字典
            saved_files: 保存的文件路径字典
        """
        logger.info("生成清洗报告...")
        
        cleaning_report = {
            'generated_at': datetime.now().isoformat(),
            'execution_summary': {
                'tables_processed': len(transformed_data),
                'tables_with_data': len([df for df in transformed_data.values() if not df.empty]),
                'total_records': sum(len(df) for df in transformed_data.values() if not df.empty),
                'saved_files': len(saved_files)
            },
            'table_details': {},
            'file_locations': {},
            'issues_and_warnings': []
        }
        
        # 各表详情
        for table_name, df in transformed_data.items():
            if df.empty:
                cleaning_report['table_details'][table_name] = {
                    'status': 'empty',
                    'record_count': 0,
                    'column_count': 0
                }
                cleaning_report['issues_and_warnings'].append(f"{table_name}: 数据为空")
            else:
                cleaning_report['table_details'][table_name] = {
                    'status': 'processed',
                    'record_count': len(df),
                    'column_count': len(df.columns),
                    'sample_record': df.iloc[0].to_dict() if len(df) > 0 else {}
                }
        
        # 文件位置
        for table_name, file_path in saved_files.items():
            cleaning_report['file_locations'][table_name] = str(file_path)
        
        # 招生数据的特殊问题
        if 'admission_data' in transformed_data:
            df = transformed_data['admission_data']
            if not df.empty:
                # 检查school_id和major_id的完整性
                if 'school_id' in df.columns and 'major_id' in df.columns:
                    school_null_count = df['school_id'].isna().sum()
                    major_null_count = df['major_id'].isna().sum()
                    
                    if school_null_count > 0 or major_null_count > 0:
                        warning = f"admission_data: {school_null_count} 条记录缺少school_id, {major_null_count} 条记录缺少major_id（需要后续匹配）"
                        cleaning_report['issues_and_warnings'].append(warning)
                else:
                    warning = f"admission_data: 缺少school_id或major_id列"
                    cleaning_report['issues_and_warnings'].append(warning)
        
        # 保存清洗报告
        report_path = OUTPUT_FILES['cleaning_report']
        try:
            with open(report_path, 'w', encoding='utf-8') as f:
                # 自定义序列化函数，处理numpy类型
                def default_serializer(obj):
                    if isinstance(obj, np.integer):
                        return int(obj)
                    elif isinstance(obj, np.floating):
                        return float(obj)
                    elif isinstance(obj, np.ndarray):
                        return obj.tolist()
                    elif isinstance(obj, pd.Timestamp):
                        return obj.isoformat()
                    elif hasattr(obj, 'isoformat'):  # 处理datetime对象
                        return obj.isoformat()
                    else:
                        raise TypeError(f"Object of type {type(obj).__name__} is not JSON serializable")
                
                json.dump(cleaning_report, f, ensure_ascii=False, indent=2, default=default_serializer)
            logger.info(f"清洗报告已保存到: {report_path}")
        except Exception as e:
            logger.error(f"保存清洗报告失败: {e}")
    
    def load_to_database(self, transformed_data: Dict[str, pd.DataFrame],
                        truncate_first: bool = True) -> Dict[str, int]:
        """
        将数据加载到MySQL数据库
        
        Args:
            transformed_data: 转换后的数据字典
            truncate_first: 是否先清空表
            
        Returns:
            各表插入的记录数
        """
        logger.info("开始加载数据到MySQL数据库...")
        
        inserted_counts = {}
        
        try:
            # 创建数据库连接
            connection = self._create_mysql_connection()
            if connection is None:
                logger.error("无法连接到MySQL数据库")
                return inserted_counts
            
            cursor = connection.cursor()
            
            # 按依赖顺序处理表
            table_order = [
                'subject_categories',
                'disciplines',
                'schools',
                'majors',
                'school_majors',
                'admission_data',
                'admins',
                'data_change_logs',
                'system_configs'
            ]
            
            for table_name in table_order:
                if table_name not in transformed_data:
                    continue
                
                df = transformed_data[table_name]
                if df.empty:
                    logger.warning(f"{table_name} 数据为空，跳过数据库加载")
                    inserted_counts[table_name] = 0
                    continue
                
                # 获取实际的数据库表名
                db_table_name = TABLE_NAMES.get(table_name, table_name)
                
                # 插入数据
                count = self._insert_data_to_mysql(cursor, df, db_table_name, truncate_first)
                inserted_counts[table_name] = count
            
            # 提交事务
            connection.commit()
            
            # 关闭连接
            cursor.close()
            connection.close()
            
            logger.info("数据加载到数据库完成!")
            
            # 打印插入统计
            total_inserted = sum(inserted_counts.values())
            logger.info(f"共插入 {total_inserted} 条记录")
            for table_name, count in inserted_counts.items():
                if count > 0:
                    logger.info(f"  {table_name}: {count} 条记录")
            
        except Error as e:
            logger.error(f"MySQL错误: {e}")
            raise
        except Exception as e:
            logger.error(f"加载数据到数据库失败: {e}")
            raise
        
        return inserted_counts
    
    def _create_mysql_connection(self):
        """创建MySQL数据库连接"""
        try:
            connection = mysql.connector.connect(
                host=DATABASE_CONFIG['host'],
                port=DATABASE_CONFIG['port'],
                user=DATABASE_CONFIG['user'],
                password=DATABASE_CONFIG['password'],
                database=DATABASE_CONFIG['database'],
                charset=DATABASE_CONFIG['charset']
            )
            
            if connection.is_connected():
                logger.info(f"成功连接到MySQL数据库: {DATABASE_CONFIG['database']}")
                return connection
            else:
                logger.error("MySQL连接失败")
                return None
                
        except Error as e:
            logger.error(f"连接MySQL数据库失败: {e}")
            return None
    
    def _insert_data_to_mysql(self, cursor, df: pd.DataFrame, table_name: str,
                             truncate_first: bool) -> int:
        """
        插入数据到MySQL表
        
        Args:
            cursor: MySQL游标
            df: 要插入的数据
            table_name: 表名
            truncate_first: 是否先清空表
            
        Returns:
            插入的记录数
        """
        if df.empty:
            return 0
        
        try:
            # 1. 如果需要，先清空表
            if truncate_first:
                truncate_sql = f"TRUNCATE TABLE {table_name};"
                cursor.execute(truncate_sql)
                logger.info(f"已清空表: {table_name}")
            
            # 2. 准备插入语句
            columns = list(df.columns)
            placeholders = ', '.join(['%s'] * len(columns))
            insert_sql = f"INSERT INTO {table_name} ({', '.join(columns)}) VALUES ({placeholders});"
            
            # 3. 准备数据
            # 将DataFrame转换为元组列表，处理NaN值为None
            data_tuples = []
            for _, row in df.iterrows():
                row_data = []
                for col in columns:
                    val = row[col]
                    # 处理NaN和NaT
                    if pd.isna(val):
                        row_data.append(None)
                    else:
                        row_data.append(val)
                data_tuples.append(tuple(row_data))
            
            # 4. 批量插入
            cursor.executemany(insert_sql, data_tuples)
            
            inserted_count = cursor.rowcount
            logger.info(f"插入 {inserted_count} 条记录到表 {table_name}")
            
            return inserted_count
            
        except Error as e:
            logger.error(f"插入数据到表 {table_name} 失败: {e}")
            # 尝试打印有问题的数据
            if 'data_tuples' in locals() and data_tuples:
                logger.debug(f"第一条数据示例: {data_tuples[0]}")
            raise
    
    def load_to_database_sqlalchemy(self, transformed_data: Dict[str, pd.DataFrame],
                                   truncate_first: bool = True) -> Dict[str, int]:
        """
        使用SQLAlchemy将数据加载到数据库（替代方法）
        
        Args:
            transformed_data: 转换后的数据字典
            truncate_first: 是否先清空表
            
        Returns:
            各表插入的记录数
        """
        logger.info("开始使用SQLAlchemy加载数据到数据库...")
        
        inserted_counts = {}
        
        try:
            # 创建SQLAlchemy引擎
            connection_string = (
                f"mysql+mysqlconnector://{DATABASE_CONFIG['user']}:{DATABASE_CONFIG['password']}"
                f"@{DATABASE_CONFIG['host']}:{DATABASE_CONFIG['port']}"
                f"/{DATABASE_CONFIG['database']}?charset={DATABASE_CONFIG['charset']}"
            )
            
            engine = create_engine(connection_string)
            
            # 按依赖顺序处理表
            table_order = [
                'subject_categories',
                'disciplines',
                'schools',
                'majors',
                'school_majors',
                'admission_data',
                'admins',
                'data_change_logs',
                'system_configs'
            ]
            
            for table_name in table_order:
                if table_name not in transformed_data:
                    continue
                
                df = transformed_data[table_name]
                if df.empty:
                    logger.warning(f"{table_name} 数据为空，跳过数据库加载")
                    inserted_counts[table_name] = 0
                    continue
                
                # 获取实际的数据库表名
                db_table_name = TABLE_NAMES.get(table_name, table_name)
                
                # 插入数据
                count = self._insert_data_sqlalchemy(engine, df, db_table_name, truncate_first)
                inserted_counts[table_name] = count
            
            # 关闭引擎
            engine.dispose()
            
            logger.info("数据加载到数据库完成!")
            
            # 打印插入统计
            total_inserted = sum(inserted_counts.values())
            logger.info(f"共插入 {total_inserted} 条记录")
            for table_name, count in inserted_counts.items():
                if count > 0:
                    logger.info(f"  {table_name}: {count} 条记录")
            
        except Exception as e:
            logger.error(f"使用SQLAlchemy加载数据失败: {e}")
            raise
        
        return inserted_counts
    
    def _insert_data_sqlalchemy(self, engine, df: pd.DataFrame, table_name: str,
                               truncate_first: bool) -> int:
        """
        使用SQLAlchemy插入数据
        
        Args:
            engine: SQLAlchemy引擎
            df: 要插入的数据
            table_name: 表名
            truncate_first: 是否先清空表
            
        Returns:
            插入的记录数
        """
        if df.empty:
            return 0
        
        try:
            # 1. 如果需要，先清空表
            if truncate_first:
                with engine.connect() as connection:
                    truncate_sql = text(f"TRUNCATE TABLE {table_name};")
                    connection.execute(truncate_sql)
                    connection.commit()
                logger.info(f"已清空表: {table_name}")
            
            # 2. 插入数据
            # 替换NaN值为None
            df = df.where(pd.notna(df), None)
            
            # 插入到数据库
            inserted_count = df.to_sql(
                name=table_name,
                con=engine,
                if_exists='append',  # 追加数据
                index=False,
                chunksize=1000
            )
            
            logger.info(f"插入 {inserted_count} 条记录到表 {table_name}")
            
            return inserted_count
            
        except Exception as e:
            logger.error(f"使用SQLAlchemy插入数据到表 {table_name} 失败: {e}")
            raise
    
    def generate_sql_insert_scripts(self, transformed_data: Dict[str, pd.DataFrame]) -> Dict[str, Path]:
        """
        生成SQL插入脚本
        
        Args:
            transformed_data: 转换后的数据字典
            
        Returns:
            SQL文件路径字典
        """
        logger.info("开始生成SQL插入脚本...")
        
        sql_files = {}
        
        try:
            # 创建SQL输出目录
            sql_dir = self.output_dir / "sql_scripts"
            sql_dir.mkdir(exist_ok=True)
            
            # 按依赖顺序处理表
            table_order = [
                'subject_categories',
                'disciplines',
                'schools',
                'majors',
                'school_majors',
                'admission_data',
                'admins',
                'data_change_logs',
                'system_configs'
            ]
            
            for table_name in table_order:
                if table_name not in transformed_data:
                    continue
                
                df = transformed_data[table_name]
                if df.empty:
                    continue
                
                # 获取实际的数据库表名
                db_table_name = TABLE_NAMES.get(table_name, table_name)
                
                # 生成SQL文件
                sql_file = sql_dir / f"insert_{table_name}.sql"
                sql_files[table_name] = sql_file
                
                # 生成SQL插入语句
                self._generate_table_sql(df, db_table_name, sql_file)
            
            # 生成主SQL文件（包含所有表的插入语句）
            main_sql_file = sql_dir / "insert_all_data.sql"
            self._generate_main_sql_file(table_order, sql_files, main_sql_file)
            sql_files['all'] = main_sql_file
            
            logger.info(f"SQL插入脚本生成完成，保存到目录: {sql_dir}")
            
        except Exception as e:
            logger.error(f"生成SQL插入脚本失败: {e}")
            raise
        
        return sql_files
    
    def _generate_table_sql(self, df: pd.DataFrame, table_name: str, output_file: Path) -> None:
        """
        生成单个表的SQL插入语句
        
        Args:
            df: 数据DataFrame
            table_name: 表名
            output_file: 输出文件路径
        """
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                # 写入文件头
                f.write(f"-- {table_name} 表数据插入脚本\n")
                f.write(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"-- 记录数: {len(df)}\n\n")
                
                # 写入插入语句
                columns = list(df.columns)
                
                for _, row in df.iterrows():
                    # 准备值列表
                    values = []
                    for col in columns:
                        val = row[col]
                        
                        # 处理不同类型的数据
                        if pd.isna(val):
                            values.append('NULL')
                        elif isinstance(val, (int, float, np.integer, np.floating)):
                            values.append(str(val))
                        elif isinstance(val, bool):
                            values.append('1' if val else '0')
                        elif isinstance(val, str):
                            # 转义单引号
                            escaped = val.replace("'", "''")
                            values.append(f"'{escaped}'")
                        else:
                            # 其他类型转换为字符串
                            escaped = str(val).replace("'", "''")
                            values.append(f"'{escaped}'")
                    
                    # 构建INSERT语句
                    insert_sql = f"INSERT INTO {table_name} ({', '.join(columns)}) VALUES ({', '.join(values)});\n"
                    f.write(insert_sql)
                
                f.write(f"\n-- 共 {len(df)} 条记录\n")
            
            logger.info(f"生成SQL文件: {output_file} ({len(df)} 条记录)")
            
        except Exception as e:
            logger.error(f"生成表 {table_name} 的SQL文件失败: {e}")
            raise
    
    def _generate_main_sql_file(self, table_order: List[str], sql_files: Dict[str, Path],
                               main_sql_file: Path) -> None:
        """
        生成主SQL文件
        
        Args:
            table_order: 表顺序列表
            sql_files: SQL文件字典
            main_sql_file: 主SQL文件路径
        """
        try:
            with open(main_sql_file, 'w', encoding='utf-8') as f:
                # 写入文件头
                f.write("-- 考研院校地图择校网站数据库数据插入脚本\n")
                f.write(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write("-- 注意: 请按顺序执行，因为有外键依赖关系\n\n")
                
                f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")
                
                # 按顺序包含各表的SQL
                for table_name in table_order:
                    if table_name in sql_files:
                        sql_file = sql_files[table_name]
                        f.write(f"-- {table_name} 表数据\n")
                        f.write(f"/* {sql_file.name} */\n\n")
                        
                        # 读取并写入内容
                        with open(sql_file, 'r', encoding='utf-8') as table_file:
                            content = table_file.read()
                            f.write(content)
                            f.write("\n\n")
                
                f.write("SET FOREIGN_KEY_CHECKS = 1;\n")
                f.write("\n-- 所有数据插入完成\n")
            
            logger.info(f"生成主SQL文件: {main_sql_file}")
            
        except Exception as e:
            logger.error(f"生成主SQL文件失败: {e}")
            raise


def test_loading():
    """测试加载功能"""
    import sys
    from .extract import ExcelExtractor
    from .transform import DataTransformer
    from .config import LOG_CONFIG
    from .utils import setup_logging
    
    # 设置日志
    setup_logging(LOG_CONFIG['file_path'])
    
    logger.info("开始测试数据加载...")
    
    try:
        # 1. 提取数据
        extractor = ExcelExtractor()
        raw_data = extractor.extract_all_data()
        
        if not any(not df.empty for df in raw_data.values()):
            logger.error("未提取到任何数据，无法进行加载测试")
            sys.exit(1)
        
        # 2. 转换数据
        transformer = DataTransformer()
        transformed_data = transformer.transform_all_data(raw_data)
        
        if not any(not df.empty for df in transformed_data.values()):
            logger.error("未转换出任何数据，无法进行加载测试")
            sys.exit(1)
        
        # 3. 加载数据（保存到文件）
        loader = DataLoader()
        saved_files = loader.save_to_files(transformed_data)
        
        if saved_files:
            logger.info("数据加载测试成功!")
            
            # 打印保存的文件
            logger.info("保存的文件:")
            for table_name, file_path in saved_files.items():
                logger.info(f"  {table_name}: {file_path}")
            
            # 测试SQL脚本生成
            sql_files = loader.generate_sql_insert_scripts(transformed_data)
            logger.info(f"生成的SQL脚本: {len(sql_files)} 个文件")
            
        else:
            logger.warning("数据加载测试失败，未保存任何文件")
            
    except Exception as e:
        logger.error(f"数据加载测试失败: {e}")
        import traceback
        logger.error(traceback.format_exc())
        sys.exit(1)


if __name__ == "__main__":
    test_loading()