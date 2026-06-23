package com.yanxiaomap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanxiaomap.entity.DataChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据修改记录表Mapper接口
 */
@Mapper
public interface DataChangeLogMapper extends BaseMapper<DataChangeLog> {
}