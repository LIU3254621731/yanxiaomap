#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
考研院校地图择校网站数据清洗主程序
负责协调数据提取、转换和加载的完整流程
"""

import argparse
import sys
from pathlib import Path
from datetime import datetime
import logging

# 导入自定义模块
from .config import LOG_CONFIG, OUTPUT_DIR
from .utils import setup_logging
from .extract import ExcelExtractor
from .transform import DataTransformer
from .load import DataLoader


class DataCleaningPipeline:
    """数据清洗管道"""
    
    def __init__(self, excel_dir: Path = None, output_dir: Path = None):
        """
        初始化数据清洗管道
        
        Args:
            excel_dir: Excel目录路径
            output_dir: 输出目录路径
        """
        self.excel_dir = excel_dir
        self.output_dir = output_dir or OUTPUT_DIR
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 设置日志
        self.log_file = self.output_dir / 'cleaning_pipeline.log'
        setup_logging(self.log_file)
        self.logger = logging.getLogger(__name__)
        
        # 初始化组件
        self.extractor = ExcelExtractor(excel_dir)
        self.transformer = DataTransformer()
        self.loader = DataLoader(self.output_dir)
        
        # 执行统计
        self.execution_stats = {
            'start_time': None,
            'end_time': None,
            'duration_seconds': None,
            'tables_processed': 0,
            'total_records': 0,
            'saved_files': 0,
            'errors': []
        }
    
    def run(self, save_to_files: bool = True, generate_sql: bool = True,
            load_to_db: bool = False, parallel_extraction: bool = True) -> bool:
        """
        运行完整的数据清洗管道
        
        Args:
            save_to_files: 是否保存到文件
            generate_sql: 是否生成SQL脚本
            load_to_db: 是否加载到数据库
            parallel_extraction: 是否并行提取数据
            
        Returns:
            是否成功
        """
        self.execution_stats['start_time'] = datetime.now()
        self.logger.info("=" * 80)
        self.logger.info("开始运行数据清洗管道")
        self.logger.info(f"开始时间: {self.execution_stats['start_time']}")
        self.logger.info("=" * 80)
        
        success = False
        
        try:
            # 步骤1: 数据提取
            self.logger.info("\n" + "=" * 80)
            self.logger.info("步骤1: 数据提取")
            self.logger.info("=" * 80)
            
            raw_data = self._extract_data(parallel_extraction)
            if not self._validate_raw_data(raw_data):
                self.logger.error("提取的数据不满足最低要求，停止执行")
                return False
            
            # 步骤2: 数据转换
            self.logger.info("\n" + "=" * 80)
            self.logger.info("步骤2: 数据转换")
            self.logger.info("=" * 80)
            
            transformed_data = self._transform_data(raw_data)
            if not self._validate_transformed_data(transformed_data):
                self.logger.error("转换后的数据不满足最低要求，停止执行")
                return False
            
            # 步骤3: 数据加载
            self.logger.info("\n" + "=" * 80)
            self.logger.info("步骤3: 数据加载")
            self.logger.info("=" * 80)
            
            if save_to_files:
                self._save_to_files(transformed_data)
            
            if generate_sql:
                self._generate_sql_scripts(transformed_data)
            
            if load_to_db:
                self._load_to_database(transformed_data)
            
            # 更新执行统计
            self._update_execution_stats(transformed_data)
            
            success = True
            
        except Exception as e:
            self.logger.error(f"数据清洗管道执行失败: {e}")
            self.execution_stats['errors'].append(str(e))
            import traceback
            self.logger.error(traceback.format_exc())
            success = False
        
        finally:
            # 记录结束时间和持续时间
            self.execution_stats['end_time'] = datetime.now()
            if self.execution_stats['start_time']:
                duration = self.execution_stats['end_time'] - self.execution_stats['start_time']
                self.execution_stats['duration_seconds'] = duration.total_seconds()
            
            # 生成执行报告
            self._generate_execution_report()
            
            self.logger.info("\n" + "=" * 80)
            self.logger.info("数据清洗管道执行完成")
            self.logger.info(f"结果: {'成功' if success else '失败'}")
            self.logger.info(f"持续时间: {self.execution_stats['duration_seconds']:.2f} 秒")
            self.logger.info("=" * 80)
        
        return success
    
    def _extract_data(self, parallel: bool) -> dict:
        """提取数据"""
        self.logger.info("开始提取Excel数据...")
        
        try:
            raw_data = self.extractor.extract_all_data()
            
            # 记录提取结果
            total_records = sum(len(df) for df in raw_data.values())
            self.logger.info(f"数据提取完成，共提取 {len(raw_data)} 个表，{total_records} 条记录")
            
            # 打印各表记录数
            for table_name, df in raw_data.items():
                status = f"{len(df)} 条记录" if not df.empty else "空"
                self.logger.info(f"  {table_name}: {status}")
            
            return raw_data
            
        except Exception as e:
            self.logger.error(f"数据提取失败: {e}")
            raise
    
    def _validate_raw_data(self, raw_data: dict) -> bool:
        """验证原始数据"""
        self.logger.info("验证原始数据...")
        
        # 检查是否有数据
        if not raw_data:
            self.logger.error("未提取到任何数据")
            return False
        
        # 检查关键表是否有数据
        key_tables = ['schools', 'majors']
        missing_key_tables = []
        
        for table in key_tables:
            if table not in raw_data or raw_data[table].empty:
                missing_key_tables.append(table)
        
        if missing_key_tables:
            self.logger.warning(f"关键表数据为空: {missing_key_tables}")
            # 不立即失败，继续处理
        
        # 计算总体数据量
        total_records = sum(len(df) for df in raw_data.values())
        if total_records == 0:
            self.logger.error("所有表都为空，没有可处理的数据")
            return False
        
        self.logger.info(f"原始数据验证通过，共 {total_records} 条记录")
        return True
    
    def _transform_data(self, raw_data: dict) -> dict:
        """转换数据"""
        self.logger.info("开始转换数据...")
        
        try:
            transformed_data = self.transformer.transform_all_data(raw_data)
            
            # 记录转换结果
            total_records = sum(len(df) for df in transformed_data.values())
            self.logger.info(f"数据转换完成，共转换 {len(transformed_data)} 个表，{total_records} 条记录")
            
            # 打印各表记录数
            for table_name, df in transformed_data.items():
                if not df.empty:
                    self.logger.info(f"  {table_name}: {len(df)} 条记录")
            
            return transformed_data
            
        except Exception as e:
            self.logger.error(f"数据转换失败: {e}")
            raise
    
    def _validate_transformed_data(self, transformed_data: dict) -> bool:
        """验证转换后的数据"""
        self.logger.info("验证转换后的数据...")
        
        # 检查是否有数据
        if not transformed_data:
            self.logger.error("未转换出任何数据")
            return False
        
        # 检查关键表是否有数据
        key_tables = ['schools', 'majors']
        has_key_data = False
        
        for table in key_tables:
            if table in transformed_data and not transformed_data[table].empty:
                has_key_data = True
                self.logger.info(f"关键表 {table} 有 {len(transformed_data[table])} 条记录")
        
        if not has_key_data:
            self.logger.error("所有关键表都为空，无法继续处理")
            return False
        
        # 计算总体数据量
        total_records = sum(len(df) for df in transformed_data.values())
        if total_records == 0:
            self.logger.error("所有表都为空，没有可加载的数据")
            return False
        
        self.logger.info(f"转换后数据验证通过，共 {total_records} 条记录")
        return True
    
    def _save_to_files(self, transformed_data: dict) -> None:
        """保存数据到文件"""
        self.logger.info("保存数据到文件...")
        
        try:
            saved_files = self.loader.save_to_files(transformed_data)
            self.execution_stats['saved_files'] = len(saved_files)
            
            self.logger.info(f"数据保存完成，共保存 {len(saved_files)} 个文件")
            
            # 打印保存的文件
            for table_name, file_path in saved_files.items():
                self.logger.info(f"  {table_name}: {file_path}")
                
        except Exception as e:
            self.logger.error(f"保存数据到文件失败: {e}")
            self.execution_stats['errors'].append(f"保存文件失败: {e}")
    
    def _generate_sql_scripts(self, transformed_data: dict) -> None:
        """生成SQL脚本"""
        self.logger.info("生成SQL插入脚本...")
        
        try:
            sql_files = self.loader.generate_sql_insert_scripts(transformed_data)
            
            if sql_files:
                self.logger.info(f"SQL脚本生成完成，共 {len(sql_files)} 个文件")
                
                # 打印生成的SQL文件
                for table_name, file_path in sql_files.items():
                    if isinstance(file_path, Path):
                        self.logger.info(f"  {table_name}: {file_path}")
            else:
                self.logger.warning("未生成任何SQL脚本")
                
        except Exception as e:
            self.logger.error(f"生成SQL脚本失败: {e}")
            self.execution_stats['errors'].append(f"生成SQL脚本失败: {e}")
    
    def _load_to_database(self, transformed_data: dict) -> None:
        """加载数据到数据库"""
        self.logger.info("加载数据到数据库...")
        
        try:
            # 检查数据库配置
            from .config import DATABASE_CONFIG
            
            # 测试数据库连接
            import mysql.connector
            try:
                connection = mysql.connector.connect(
                    host=DATABASE_CONFIG['host'],
                    port=DATABASE_CONFIG['port'],
                    user=DATABASE_CONFIG['user'],
                    password=DATABASE_CONFIG['password']
                )
                if connection.is_connected():
                    self.logger.info("数据库连接测试成功")
                    connection.close()
                else:
                    self.logger.warning("数据库连接测试失败，跳过数据库加载")
                    return
            except Exception as e:
                self.logger.warning(f"数据库连接测试失败: {e}，跳过数据库加载")
                return
            
            # 加载数据到数据库
            inserted_counts = self.loader.load_to_database(transformed_data, truncate_first=True)
            
            total_inserted = sum(inserted_counts.values())
            self.logger.info(f"数据加载到数据库完成，共插入 {total_inserted} 条记录")
            
            # 打印插入统计
            for table_name, count in inserted_counts.items():
                if count > 0:
                    self.logger.info(f"  {table_name}: {count} 条记录")
                    
        except Exception as e:
            self.logger.error(f"加载数据到数据库失败: {e}")
            self.execution_stats['errors'].append(f"加载到数据库失败: {e}")
    
    def _update_execution_stats(self, transformed_data: dict) -> None:
        """更新执行统计"""
        self.execution_stats['tables_processed'] = len(transformed_data)
        self.execution_stats['total_records'] = sum(
            len(df) for df in transformed_data.values()
        )
    
    def _generate_execution_report(self) -> None:
        """生成执行报告"""
        report_path = self.output_dir / 'execution_report.json'
        
        try:
            import json
            
            report = {
                'pipeline_execution': {
                    'start_time': self.execution_stats['start_time'].isoformat() if self.execution_stats['start_time'] else None,
                    'end_time': self.execution_stats['end_time'].isoformat() if self.execution_stats['end_time'] else None,
                    'duration_seconds': self.execution_stats['duration_seconds'],
                    'success': len(self.execution_stats['errors']) == 0
                },
                'data_statistics': {
                    'tables_processed': self.execution_stats['tables_processed'],
                    'total_records': self.execution_stats['total_records'],
                    'saved_files': self.execution_stats['saved_files']
                },
                'errors': self.execution_stats['errors'],
                'output_directory': str(self.output_dir),
                'log_file': str(self.log_file)
            }
            
            with open(report_path, 'w', encoding='utf-8') as f:
                json.dump(report, f, ensure_ascii=False, indent=2)
            
            self.logger.info(f"执行报告已保存到: {report_path}")
            
        except Exception as e:
            self.logger.error(f"生成执行报告失败: {e}")


def parse_arguments():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(
        description='考研院校地图择校网站数据清洗管道',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python main.py                       # 运行完整管道，保存到文件
  python main.py --no-sql              # 不生成SQL脚本
  python main.py --load-to-db          # 加载到数据库（需要配置数据库连接）
  python main.py --excel-dir ./data    # 指定Excel目录
        """
    )
    
    parser.add_argument(
        '--excel-dir',
        type=Path,
        help='Excel数据目录路径'
    )
    
    parser.add_argument(
        '--output-dir',
        type=Path,
        default=OUTPUT_DIR,
        help='输出目录路径'
    )
    
    parser.add_argument(
        '--no-files',
        action='store_false',
        dest='save_to_files',
        help='不保存到文件'
    )
    
    parser.add_argument(
        '--no-sql',
        action='store_false',
        dest='generate_sql',
        help='不生成SQL脚本'
    )
    
    parser.add_argument(
        '--load-to-db',
        action='store_true',
        help='加载数据到数据库'
    )
    
    parser.add_argument(
        '--sequential',
        action='store_false',
        dest='parallel_extraction',
        help='顺序提取数据（而不是并行）'
    )
    
    parser.add_argument(
        '--log-level',
        choices=['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL'],
        default='INFO',
        help='日志级别'
    )
    
    return parser.parse_args()


def main():
    """主函数"""
    args = parse_arguments()
    
    # 设置日志级别
    logging.getLogger().setLevel(getattr(logging, args.log_level))
    
    # 创建并运行管道
    pipeline = DataCleaningPipeline(
        excel_dir=args.excel_dir,
        output_dir=args.output_dir
    )
    
    success = pipeline.run(
        save_to_files=args.save_to_files,
        generate_sql=args.generate_sql,
        load_to_db=args.load_to_db,
        parallel_extraction=args.parallel_extraction
    )
    
    # 返回退出码
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()