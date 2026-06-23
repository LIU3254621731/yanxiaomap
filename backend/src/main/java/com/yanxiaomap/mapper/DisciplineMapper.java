package com.yanxiaomap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanxiaomap.entity.Discipline;
import org.apache.ibatis.annotations.Mapper;

/**
 * 一级学科表Mapper接口
 */
@Mapper
public interface DisciplineMapper extends BaseMapper<Discipline> {
}