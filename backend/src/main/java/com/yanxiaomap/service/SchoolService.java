package com.yanxiaomap.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yanxiaomap.entity.School;
import java.util.List;
import java.util.Map;

/**
 * 院校服务接口
 */
public interface SchoolService extends IService<School> {

    /**
     * 根据条件查询院校列表（地图筛选）
     */
    List<School> getSchoolsForMap(Map<String, Object> params);

    /**
     * 综合搜索院校（支持分页、排序、多条件）
     */
    Page<School> searchSchools(Map<String, Object> params);

    /**
     * 根据ID获取院校详情（包含关联信息）
     */
    Map<String, Object> getSchoolDetail(Integer schoolId);

    /**
     * 批量获取院校信息
     */
    List<School> getSchoolsByIds(List<Integer> schoolIds);

    /**
     * 获取院校数量统计
     */
    Map<String, Long> getSchoolStatistics();

    /**
     * 检查院校代码是否已存在
     */
    boolean checkCodeExists(String code, Integer excludeId);

    /**
     * 根据经纬度范围查询院校
     */
    List<School> getSchoolsByLocation(Double minLng, Double maxLng, Double minLat, Double maxLat);
}