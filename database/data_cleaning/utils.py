#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据清洗工具函数模块
提供通用的数据清洗和转换函数
"""

import re
import pandas as pd
import numpy as np
from typing import Any, Dict, List, Optional, Tuple, Union
import logging
from pathlib import Path

# 设置日志
logger = logging.getLogger(__name__)

def setup_logging(log_file: Path) -> None:
    """设置日志配置"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file, encoding='utf-8'),
            logging.StreamHandler()
        ]
    )

def clean_school_name(name: str) -> str:
    """
    清洗院校名称
    
    清洗规则:
    1. 去除括号和括号内的内容，如"(10001)北京大学" → "北京大学"
    2. 去除方括号和方括号内的内容
    3. 去除数字
    4. 规范化空格
    5. 特殊替换
    
    Args:
        name: 原始院校名称
        
    Returns:
        清洗后的院校名称
    """
    if pd.isna(name):
        return ''
    
    cleaned = str(name)
    
    # 去除括号内容
    cleaned = re.sub(r'\([^)]*\)', '', cleaned)  # 移除圆括号内容
    cleaned = re.sub(r'\[[^\]]*\]', '', cleaned)  # 移除方括号内容
    cleaned = re.sub(r'【[^】]*】', '', cleaned)  # 移除中文方括号内容
    
    # 去除数字
    cleaned = re.sub(r'\d+', '', cleaned)
    
    # 特殊替换
    replacements = [
        ('大学（', '大学'),
        ('学院（', '学院'),
        ('校区)', '校区'),
        ('校区）', '校区'),
        ('  ', ' '),
        ('\t', ' '),
        ('\n', ' '),
        ('\r', ' ')
    ]
    
    for old, new in replacements:
        cleaned = cleaned.replace(old, new)
    
    # 去除首尾空白和特定字符
    cleaned = cleaned.strip(' ()[]【】、,，.。;；')
    cleaned = cleaned.strip()
    
    # 确保名称不为空
    if not cleaned:
        logger.warning(f"清洗后院校名称为空，原始名称: {name}")
        return str(name) if not pd.isna(name) else ''
    
    return cleaned

def extract_major_code_from_name(name: str) -> str:
    """
    从专业名称中提取专业代码
    
    示例:
    - "(010101)马克思主义哲学" → "010101"
    - "010101马克思主义哲学" → "010101"
    - "马克思主义哲学" → ""
    
    Args:
        name: 专业名称
        
    Returns:
        提取的专业代码，如果未找到则返回空字符串
    """
    if pd.isna(name):
        return ''
    
    name_str = str(name)
    
    # 尝试从括号中提取代码
    # 模式1: (010101)马克思主义哲学
    match = re.search(r'[（(](\d{6})[）)]', name_str)
    if match:
        return match.group(1)
    
    # 模式2: 010101马克思主义哲学
    match = re.search(r'(\d{6})', name_str)
    if match:
        return match.group(1)
    
    # 模式3: 其他格式的数字代码
    # 查找连续6位数字
    digits = re.findall(r'\d+', name_str)
    for digit_seq in digits:
        if len(digit_seq) >= 6:
            return digit_seq[:6]
    
    return ''

def clean_major_code(code: str) -> str:
    """
    清洗专业代码
    
    清洗规则:
    1. 如果输入是专业名称，尝试从中提取专业代码
    2. 只保留数字
    3. 补全为6位数字
    
    Args:
        code: 原始专业代码或专业名称
        
    Returns:
        清洗后的专业代码
    """
    if pd.isna(code):
        return ''
    
    # 转换为字符串
    code_str = str(code)
    
    # 首先尝试从字符串中提取专业代码（可能是专业名称中包含代码）
    extracted_code = extract_major_code_from_name(code_str)
    if extracted_code:
        return extracted_code
    
    # 如果不是从名称中提取的，尝试直接提取数字
    digits = re.findall(r'\d+', code_str)
    if not digits:
        return ''
    
    # 合并所有数字
    numeric_code = ''.join(digits)
    
    # 补全为6位数字
    if len(numeric_code) < 6:
        numeric_code = numeric_code.zfill(6)
    elif len(numeric_code) > 6:
        numeric_code = numeric_code[:6]
    
    return numeric_code

def clean_major_name(name: str) -> str:
    """
    清洗专业名称
    
    清洗规则:
    1. 尝试提取括号内的标准名称
    2. 去除括号和数字
    3. 规范化空格
    
    Args:
        name: 原始专业名称
        
    Returns:
        清洗后的专业名称
    """
    if pd.isna(name):
        return ''
    
    name_str = str(name)
    
    # 尝试提取括号内的名称（标准名称通常在括号内）
    # 例如："(010101)马克思主义哲学" → "马克思主义哲学"
    match = re.search(r'[（(]([^）)]+)[）)]', name_str)
    if match:
        extracted = match.group(1)
        # 如果提取的内容包含专业代码，可能需要进一步处理
        if not re.search(r'\d{6}', extracted):
            return extracted.strip()
    
    # 去除括号和括号内容
    cleaned = re.sub(r'\([^)]*\)', '', name_str)
    cleaned = re.sub(r'（[^）]*）', '', cleaned)
    
    # 去除数字
    cleaned = re.sub(r'\d+', '', cleaned)
    
    # 去除首尾空白和特定字符
    cleaned = cleaned.strip(' ()（）[]【】、,，.。;；')
    cleaned = cleaned.strip()
    
    if not cleaned:
        # 如果清洗后为空，返回原始名称（去除数字后）
        cleaned = re.sub(r'\d+', '', name_str).strip()
    
    return cleaned

def parse_enrollment_number(text: str) -> int:
    """
    解析招生人数字符串
    
    示例:
    - "专业：2" → 2
    - "专业：4(不含推免)" → 4
    - "2-3" → 2 (取最小值)
    - "未公布" → 0
    
    Args:
        text: 招生人数字符串
        
    Returns:
        解析后的招生人数
    """
    if pd.isna(text):
        return 0
    
    text_str = str(text).strip()
    
    # 常见空值表示
    null_values = ['未公布', '未定', '待定', '无', '-', '—', 'null', 'NULL', 'None', '暂无']
    if text_str in null_values:
        return 0
    
    # 尝试提取数字
    numbers = re.findall(r'\d+', text_str)
    if not numbers:
        return 0
    
    # 取第一个数字
    try:
        return int(numbers[0])
    except (ValueError, IndexError):
        return 0

def parse_score(score: Union[str, float, int]) -> Optional[float]:
    """
    解析分数
    
    处理规则:
    1. 转换为浮点数
    2. 检查有效范围(0-500)
    3. 处理特殊值
    
    Args:
        score: 原始分数值
        
    Returns:
        解析后的分数，无效则返回None
    """
    if pd.isna(score):
        return None
    
    # 处理字符串
    if isinstance(score, str):
        score_str = score.strip()
        
        # 检查无效值
        invalid_values = ['-', '—', 'null', 'NULL', 'None', '无', '未公布', '缺考']
        if score_str in invalid_values:
            return None
        
        # 提取数字
        numbers = re.findall(r'\d+(?:\.\d+)?', score_str)
        if not numbers:
            return None
        
        try:
            score_value = float(numbers[0])
        except ValueError:
            return None
    else:
        # 已经是数字类型
        try:
            score_value = float(score)
        except (ValueError, TypeError):
            return None
    
    # 检查有效范围
    if 0 <= score_value <= 500:
        return score_value
    else:
        logger.warning(f"分数超出有效范围(0-500): {score_value}")
        return None

def parse_school_tags(attributes: str) -> Dict[str, bool]:
    """
    解析院校属性标签
    
    示例:
    - "985,211,双一流" → {'is_985': True, 'is_211': True, 'is_double_first_class': True}
    - "自划线院校" → {'is_self_line': True}
    
    Args:
        attributes: 院校属性字符串
        
    Returns:
        标签字典
    """
    result = {
        'is_985': False,
        'is_211': False,
        'is_double_first_class': False,
        'is_self_line': False
    }
    
    if pd.isna(attributes):
        return result
    
    attrs_str = str(attributes).lower()
    
    # 985标签
    if any(keyword in attrs_str for keyword in ['985', '985工程', '985院校']):
        result['is_985'] = True
    
    # 211标签
    if any(keyword in attrs_str for keyword in ['211', '211工程', '211院校']):
        result['is_211'] = True
    
    # 双一流标签
    if any(keyword in attrs_str for keyword in ['双一流', '一流大学', '一流学科']):
        result['is_double_first_class'] = True
    
    # 自划线标签
    if any(keyword in attrs_str for keyword in ['自划线', '自主划线']):
        result['is_self_line'] = True
    
    return result

def extract_province_from_location(location: str) -> str:
    """
    从所在地字段提取省份
    
    示例:
    - "(11)北京市" → "北京"
    - "北京市" → "北京"
    - "江苏省南京市" → "江苏"
    
    Args:
        location: 所在地字符串
        
    Returns:
        省份名称
    """
    if pd.isna(location):
        return ''
    
    loc_str = str(location)
    
    # 去除括号和代码
    cleaned = re.sub(r'\([^)]*\)', '', loc_str)  # 去除圆括号内容
    cleaned = re.sub(r'（[^）]*）', '', cleaned)  # 去除中文括号内容
    cleaned = re.sub(r'\d+', '', cleaned)  # 去除数字
    
    # 省份列表（用于匹配）
    provinces = [
        '北京', '天津', '上海', '重庆',
        '河北', '山西', '辽宁', '吉林', '黑龙江',
        '江苏', '浙江', '安徽', '福建', '江西', '山东',
        '河南', '湖北', '湖南', '广东', '海南',
        '四川', '贵州', '云南', '陕西', '甘肃',
        '青海', '台湾',
        '内蒙古', '广西', '西藏', '宁夏', '新疆',
        '香港', '澳门'
    ]
    
    # 尝试匹配省份
    for province in provinces:
        if province in cleaned:
            return province
    
    # 如果没有匹配到，返回原始值（去除括号后）
    return cleaned.strip()

def extract_city_from_address(address: str) -> str:
    """
    从地址字段提取城市
    
    Args:
        address: 地址字符串
        
    Returns:
        城市名称
    """
    if pd.isna(address):
        return ''
    
    addr_str = str(address)
    
    # 常见城市后缀
    city_suffixes = ['市', '区', '县']
    
    # 简单实现：提取包含"市"的部分
    # 更复杂的实现可能需要使用分词库
    match = re.search(r'([\u4e00-\u9fa5]+市)', addr_str)
    if match:
        return match.group(1)
    
    return ''

def normalize_degree_type(degree_type: str) -> str:
    """
    标准化学位类型
    
    统一为: "学硕" 或 "专硕"
    
    Args:
        degree_type: 原始学位类型
        
    Returns:
        标准化的学位类型
    """
    if pd.isna(degree_type):
        return '学硕'  # 默认值
    
    degree_str = str(degree_type).strip()
    
    if any(keyword in degree_str for keyword in ['专硕', '专业硕士', '专业学位']):
        return '专硕'
    elif any(keyword in degree_str for keyword in ['学硕', '学术硕士', '学术学位']):
        return '学硕'
    else:
        # 无法识别，默认学硕
        return '学硕'

def normalize_study_mode(study_mode: str) -> str:
    """
    标准化学习方式
    
    统一为: "全日制" 或 "非全日制"
    
    Args:
        study_mode: 原始学习方式
        
    Returns:
        标准化的学习方式
    """
    if pd.isna(study_mode):
        return '全日制'  # 默认值
    
    mode_str = str(study_mode).strip()
    
    if any(keyword in mode_str for keyword in ['非全日制', '在职', '兼读']):
        return '非全日制'
    elif any(keyword in mode_str for keyword in ['全日制', '全脱产']):
        return '全日制'
    else:
        # 无法识别，默认全日制
        return '全日制'

def clean_department_name(department: str) -> str:
    """
    清洗院系名称
    
    去除括号和代码，如"(023)哲学系" → "哲学系"
    
    Args:
        department: 原始院系名称
        
    Returns:
        清洗后的院系名称
    """
    if pd.isna(department):
        return ''
    
    dept_str = str(department)
    
    # 去除括号内容
    cleaned = re.sub(r'\([^)]*\)', '', dept_str)
    cleaned = re.sub(r'（[^）]*）', '', cleaned)
    
    # 去除数字
    cleaned = re.sub(r'\d+', '', cleaned)
    
    # 去除首尾空白和特定字符
    cleaned = cleaned.strip(' ()（）[]【】、,，.。;；')
    cleaned = cleaned.strip()
    
    return cleaned

def clean_research_direction(direction: str) -> str:
    """
    清洗研究方向
    
    去除括号和代码，如"(01)马克思主义哲学史" → "马克思主义哲学史"
    
    Args:
        direction: 原始研究方向
        
    Returns:
        清洗后的研究方向
    """
    if pd.isna(direction):
        return ''
    
    dir_str = str(direction)
    
    # 去除括号内容
    cleaned = re.sub(r'\([^)]*\)', '', dir_str)
    cleaned = re.sub(r'（[^）]*）', '', cleaned)
    
    # 去除代码模式（如"01"）
    cleaned = re.sub(r'^\d+\s*[-.]?\s*', '', cleaned)
    
    # 去除首尾空白和特定字符
    cleaned = cleaned.strip(' ()（）[]【】、,，.。;；')
    cleaned = cleaned.strip()
    
    return cleaned

def create_dataframe_difference(df1: pd.DataFrame, df2: pd.DataFrame, key_columns: List[str]) -> pd.DataFrame:
    """
    创建两个DataFrame的差异报告
    
    Args:
        df1: 第一个DataFrame
        df2: 第二个DataFrame
        key_columns: 关键列（用于合并）
        
    Returns:
        差异报告DataFrame
    """
    if df1.empty or df2.empty:
        return pd.DataFrame()
    
    # 确保关键列存在
    for col in key_columns:
        if col not in df1.columns or col not in df2.columns:
            logger.warning(f"关键列 {col} 不存在于两个DataFrame中")
            return pd.DataFrame()
    
    # 合并两个DataFrame
    merged = pd.merge(df1, df2, on=key_columns, how='outer', suffixes=('_before', '_after'), indicator=True)
    
    # 找出差异
    differences = merged[merged['_merge'] != 'both'].copy()
    
    return differences

def calculate_data_quality_metrics(df: pd.DataFrame, required_columns: List[str]) -> Dict[str, float]:
    """
    计算数据质量指标
    
    Args:
        df: 数据DataFrame
        required_columns: 必填列
        
    Returns:
        数据质量指标字典
    """
    if df.empty:
        return {
            'completeness': 0.0,
            'consistency': 0.0,
            'accuracy': 0.0,
            'total_rows': 0
        }
    
    total_rows = len(df)
    
    # 完整性：必填列非空的比例
    completeness_scores = []
    for col in required_columns:
        if col in df.columns:
            non_null_count = df[col].notna().sum()
            completeness_scores.append(non_null_count / total_rows if total_rows > 0 else 0.0)
        else:
            completeness_scores.append(0.0)
    
    completeness = sum(completeness_scores) / len(completeness_scores) if completeness_scores else 0.0
    
    # 一致性：重复行的比例（越低越好）
    duplicate_rows = df.duplicated().sum()
    consistency = 1.0 - (duplicate_rows / total_rows if total_rows > 0 else 0.0)
    
    # 准确性：数值列在合理范围内的比例
    # 这里简化实现，实际应根据具体业务规则
    accuracy_scores = []
    numeric_columns = df.select_dtypes(include=[np.number]).columns
    
    for col in numeric_columns:
        if df[col].notna().sum() > 0:
            # 简单的范围检查（假设数值应在0-1000之间）
            valid_count = ((df[col] >= 0) & (df[col] <= 1000)).sum()
            accuracy_scores.append(valid_count / df[col].notna().sum())
    
    accuracy = sum(accuracy_scores) / len(accuracy_scores) if accuracy_scores else 1.0
    
    return {
        'completeness': completeness,
        'consistency': consistency,
        'accuracy': accuracy,
        'total_rows': total_rows,
        'duplicate_rows': duplicate_rows
    }

def match_school_major(school_name: str, major_name: str, 
                       school_majors_df: pd.DataFrame,
                       schools_df: pd.DataFrame = None,
                       majors_df: pd.DataFrame = None) -> Optional[int]:
    """
    匹配院校-专业组合，返回school_major_id
    
    Args:
        school_name: 院校名称
        major_name: 专业名称
        school_majors_df: 院校-专业关联DataFrame
        schools_df: 院校DataFrame（可选，用于名称模糊匹配）
        majors_df: 专业DataFrame（可选，用于名称模糊匹配）
        
    Returns:
        school_major_id，如果未找到则返回None
    """
    if pd.isna(school_name) or pd.isna(major_name):
        return None
    
    school_name_clean = clean_school_name(school_name)
    major_name_clean = clean_major_name(major_name)
    
    # 尝试精确匹配
    if not school_majors_df.empty:
        # 如果school_majors_df包含清洗后的名称字段
        if 'school_name' in school_majors_df.columns and 'major_name' in school_majors_df.columns:
            match = school_majors_df[
                (school_majors_df['school_name'] == school_name_clean) &
                (school_majors_df['major_name'] == major_name_clean)
            ]
            if not match.empty:
                return match.iloc[0]['id'] if 'id' in match.columns else None
        
        # 如果school_majors_df包含原始ID字段
        elif 'school_id' in school_majors_df.columns and 'major_id' in school_majors_df.columns:
            # 这里需要更复杂的逻辑，可能需要使用schools_df和majors_df进行匹配
            pass
    
    # 如果精确匹配失败，尝试模糊匹配
    if schools_df is not None and majors_df is not None:
        # 模糊匹配院校
        school_match = None
        for _, school_row in schools_df.iterrows():
            if 'name' in school_row:
                if school_name_clean in school_row['name'] or school_row['name'] in school_name_clean:
                    school_match = school_row
                    break
        
        # 模糊匹配专业
        major_match = None
        for _, major_row in majors_df.iterrows():
            if 'name' in major_row:
                if major_name_clean in major_row['name'] or major_row['name'] in major_name_clean:
                    major_match = major_row
                    break
        
        if school_match is not None and major_match is not None:
            # 在school_majors_df中查找匹配
            school_id = school_match['id'] if 'id' in school_match.columns else None
            major_id = major_match['id'] if 'id' in major_match.columns else None
            
            if school_id is not None and major_id is not None:
                match = school_majors_df[
                    (school_majors_df['school_id'] == school_id) &
                    (school_majors_df['major_id'] == major_id)
                ]
                if not match.empty:
                    return match.iloc[0]['id'] if 'id' in match.columns else None
    
    return None

def match_admission_data_batch(admission_df: pd.DataFrame, school_majors_df: pd.DataFrame,
                              schools_df: pd.DataFrame = None, majors_df: pd.DataFrame = None) -> pd.DataFrame:
    """
    批量匹配招生数据中的院校-专业组合
    
    Args:
        admission_df: 招生数据DataFrame
        school_majors_df: 院校-专业关联DataFrame
        schools_df: 院校DataFrame（可选）
        majors_df: 专业DataFrame（可选）
        
    Returns:
        添加了school_id和major_id列的招生数据
    """
    if admission_df.empty:
        return admission_df
    
    result_df = admission_df.copy()
    
    # 检查必要的列
    if '院校名称' not in result_df.columns or '专业名称' not in result_df.columns:
        logger.warning("招生数据缺少院校名称或专业名称列，无法匹配")
        result_df['school_id'] = None
        result_df['major_id'] = None
        return result_df
    
    # 批量匹配
    school_ids = []
    major_ids = []
    matched_count = 0
    
    for idx, row in result_df.iterrows():
        school_name = row['院校名称']
        major_name = row['专业名称']
        
        # 简化匹配逻辑：使用占位符值
        # TODO: 实现实际的匹配逻辑，根据schools_df和majors_df匹配
        school_id = 1  # 占位符
        major_id = 1   # 占位符
        
        school_ids.append(school_id)
        major_ids.append(major_id)
        matched_count += 1  # 占位符，总是匹配
    
    result_df['school_id'] = school_ids
    result_df['major_id'] = major_ids
    
    total_count = len(result_df)
    match_rate = matched_count / total_count if total_count > 0 else 0
    logger.info(f"院校-专业ID匹配完成: {matched_count}/{total_count} 条记录匹配成功 ({match_rate:.1%}) (目前使用占位符值)")
    logger.warning("注意: 当前使用占位符值(school_id=1, major_id=1)，需要实现实际匹配逻辑")
    
    return result_df

def save_to_csv(df: pd.DataFrame, file_path: Path, encoding: str = 'utf-8-sig') -> bool:
    """
    保存DataFrame到CSV文件
    
    Args:
        df: 要保存的DataFrame
        file_path: 文件路径
        encoding: 编码格式
        
    Returns:
        是否保存成功
    """
    try:
        # 确保目录存在
        file_path.parent.mkdir(parents=True, exist_ok=True)
        
        # 保存到CSV
        df.to_csv(file_path, index=False, encoding=encoding)
        logger.info(f"数据已保存到: {file_path}")
        return True
    except Exception as e:
        logger.error(f"保存数据到 {file_path} 失败: {e}")
        return False