package com.yanxiaomap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanxiaomap.entity.School;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院校表Mapper接口
 */
@Mapper
public interface SchoolMapper extends BaseMapper<School> {
    // 可以在此处添加自定义的SQL查询方法
    // 例如：List<School> selectByCondition(Map<String, Object> params);
}