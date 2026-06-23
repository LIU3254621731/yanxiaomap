#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Excel文件分析脚本
用于分析考研数据Excel文件的结构和内容
"""

import pandas as pd
import os
from pathlib import Path
import json

def analyze_excel_file(file_path):
    """分析单个Excel文件"""
    print(f"\n{'='*80}")
    print(f"分析文件: {file_path}")
    print(f"{'='*80}")
    
    try:
        # 获取所有工作表名称
        xl = pd.ExcelFile(file_path)
        sheet_names = xl.sheet_names
        print(f"工作表数量: {len(sheet_names)}")
        print(f"工作表名称: {sheet_names}")
        
        results = []
        for sheet_name in sheet_names:
            print(f"\n--- 工作表: {sheet_name} ---")
            try:
                # 读取前5行数据
                df = xl.parse(sheet_name, nrows=10)
                print(f"数据形状: {df.shape}")
                print(f"列名: {list(df.columns)}")
                print(f"前3行数据:")
                print(df.head(3).to_string(index=False))
                
                # 收集分析结果
                sheet_info = {
                    'sheet_name': sheet_name,
                    'shape': df.shape,
                    'columns': list(df.columns),
                    'dtypes': {col: str(dtype) for col, dtype in df.dtypes.items()},
                    'sample_data': df.head(3).to_dict(orient='records')
                }
                results.append(sheet_info)
                
            except Exception as e:
                print(f"读取工作表 {sheet_name} 出错: {e}")
                results.append({
                    'sheet_name': sheet_name,
                    'error': str(e)
                })
        
        return {
            'file_path': str(file_path),
            'sheet_names': sheet_names,
            'sheets': results
        }
        
    except Exception as e:
        print(f"读取Excel文件出错: {e}")
        return {
            'file_path': str(file_path),
            'error': str(e)
        }

def main():
    """主函数"""
    # Excel目录路径
    excel_dir = Path(r"c:\Users\32546\Desktop\yanxiaomap\root\mydate17_24\excel")
    
    # 关键文件列表（优先分析）
    key_files = [
        excel_dir / "报考" / "04-硕士研究生招生目录(2024).xlsx",
        excel_dir / "报考" / "22年.xlsx",
        excel_dir / "报考" / "23年.xlsx",
        excel_dir / "复试成绩" / "17-20研究生复试分数线.xlsx",
        excel_dir / "复试成绩" / "20-22研究生复试分数线.xlsx",
        excel_dir / "复试成绩" / "2023研究生复试分数线.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "2024年全国高等院校硕士研究生复试分数线.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "04-硕士研究生招生目录(2024).xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "2023研究生复试分数线.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "20-22研究生复试分数线.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "2024年全国高等院校硕士研究生的招生计划.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "历年考研复试分数线" / "17-20研究生复试分数线.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "23年.xlsx",
        excel_dir / "考研数据" / "考研复试分数线" / "考研招生目录+择校数据" / "22年.xlsx"
    ]
    
    all_results = []
    
    # 分析关键文件
    for file_path in key_files:
        if file_path.exists():
            result = analyze_excel_file(file_path)
            all_results.append(result)
        else:
            print(f"\n文件不存在: {file_path}")
    
    # 保存分析结果到JSON文件
    output_file = Path(__file__).parent / "excel_analysis_report.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(all_results, f, ensure_ascii=False, indent=2)
    
    print(f"\n{'='*80}")
    print(f"分析完成！结果已保存到: {output_file}")
    print(f"分析了 {len([r for r in all_results if 'error' not in r])} 个文件")
    
    # 生成简要报告
    print("\n简要报告:")
    for result in all_results:
        if 'error' in result:
            print(f"- {Path(result['file_path']).name}: 错误 - {result['error']}")
        else:
            print(f"- {Path(result['file_path']).name}: {len(result['sheet_names'])} 个工作表")
            for sheet in result['sheets']:
                if 'error' in sheet:
                    print(f"  - {sheet['sheet_name']}: 错误 - {sheet['error']}")
                else:
                    print(f"  - {sheet['sheet_name']}: {sheet['shape'][1]} 列, {sheet['shape'][0]} 行样本")

if __name__ == "__main__":
    main()