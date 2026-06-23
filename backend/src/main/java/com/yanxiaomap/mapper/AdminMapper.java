package com.yanxiaomap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanxiaomap.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员表Mapper接口
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}