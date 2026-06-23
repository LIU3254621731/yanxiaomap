package com.yanxiaomap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanxiaomap.entity.SchoolMajor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院校-专业关联表Mapper接口
 */
@Mapper
public interface SchoolMajorMapper extends BaseMapper<SchoolMajor> {
}