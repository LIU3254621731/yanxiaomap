package com.yanxiaomap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yanxiaomap.entity.AdmissionData;
import com.yanxiaomap.entity.Major;
import com.yanxiaomap.entity.School;
import com.yanxiaomap.entity.SchoolMajor;
import com.yanxiaomap.mapper.SchoolMajorMapper;
import com.yanxiaomap.mapper.SchoolMapper;
import com.yanxiaomap.service.AdmissionDataService;
import com.yanxiaomap.service.SchoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 院校服务实现类
 */
@Slf4j
@Service
public class SchoolServiceImpl extends ServiceImpl<SchoolMapper, School> implements SchoolService {

    @Autowired
    private SchoolMajorMapper schoolMajorMapper;

    @Autowired
    private AdmissionDataService admissionDataService;

    @Override
    public List<School> getSchoolsForMap(Map<String, Object> params) {
        LambdaQueryWrapper<School> queryWrapper = buildMapQueryWrapper(params);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Page<School> searchSchools(Map<String, Object> params) {
        LambdaQueryWrapper<School> queryWrapper = buildSearchQueryWrapper(params);
        
        Integer page = (Integer) params.getOrDefault("page", 1);
        Integer size = (Integer) params.getOrDefault("size", 20);
        String sortField = (String) params.getOrDefault("sortField", "id");
        String sortOrder = (String) params.getOrDefault("sortOrder", "desc");
        
        // 构建分页对象
        Page<School> pageObj = new Page<>(page, size);
        
        // 设置排序
        if ("asc".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc(getSortField(sortField));
        } else {
            queryWrapper.orderByDesc(getSortField(sortField));
        }
        
        return baseMapper.selectPage(pageObj, queryWrapper);
    }

    @Override
    public Map<String, Object> getSchoolDetail(Integer schoolId) {
        log.info("查询院校详情: schoolId={}", schoolId);
        Map<String, Object> result = new HashMap<>();
        
        if (schoolId == null) {
            log.warn("查询院校详情失败: schoolId不能为空");
            result.put("success", false);
            result.put("message", "院校ID不能为空");
            return result;
        }
        
        // 查询院校基本信息
        School school = baseMapper.selectById(schoolId);
        if (school == null) {
            log.warn("查询院校详情失败: 院校不存在 schoolId={}", schoolId);
            result.put("success", false);
            result.put("message", "院校不存在");
            return result;
        }
        
        // 构建院校信息（可以排除敏感或不需要的字段）
        Map<String, Object> schoolInfo = new HashMap<>();
        schoolInfo.put("id", school.getId());
        schoolInfo.put("code", school.getCode());
        schoolInfo.put("name", school.getName());
        schoolInfo.put("enrollmentUnit", school.getEnrollmentUnit());
        schoolInfo.put("level", school.getLevel());
        schoolInfo.put("type", school.getType());
        schoolInfo.put("belong", school.getBelong());
        schoolInfo.put("province", school.getProvince());
        schoolInfo.put("city", school.getCity());
        schoolInfo.put("address", school.getAddress());
        schoolInfo.put("longitude", school.getLongitude());
        schoolInfo.put("latitude", school.getLatitude());
        schoolInfo.put("introduction", school.getIntroduction());
        schoolInfo.put("website", school.getWebsite());
        schoolInfo.put("establishedYear", school.getEstablishedYear());
        schoolInfo.put("status", school.getStatus());
        schoolInfo.put("createdAt", school.getCreatedAt());
        schoolInfo.put("updatedAt", school.getUpdatedAt());
        
        result.put("success", true);
        result.put("school", schoolInfo);
        result.put("message", "查询成功");

        // 查询该院校开设的专业列表
        LambdaQueryWrapper<SchoolMajor> smWrapper = Wrappers.lambdaQuery();
        smWrapper.eq(SchoolMajor::getSchoolId, schoolId);
        List<SchoolMajor> schoolMajors = schoolMajorMapper.selectList(smWrapper);
        List<Integer> majorIds = schoolMajors.stream()
                .map(SchoolMajor::getMajorId)
                .collect(Collectors.toList());
        result.put("majorCount", majorIds.size());
        result.put("majorIds", majorIds);

        // 查询该院校的招生数据
        List<AdmissionData> admissionDataList = admissionDataService.getBySchoolId(schoolId);
        result.put("admissionData", admissionDataList);

        // 统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("majorCount", majorIds.size());
        statistics.put("admissionDataCount", admissionDataList.size());
        if (!admissionDataList.isEmpty()) {
            double avgScore = admissionDataList.stream()
                    .mapToInt(AdmissionData::getAverageAdmissionScore)
                    .average()
                    .orElse(0.0);
            int totalEnrollment = admissionDataList.stream()
                    .mapToInt(AdmissionData::getActualEnroll)
                    .sum();
            statistics.put("averageScore", avgScore);
            statistics.put("totalEnrollment", totalEnrollment);
        }
        result.put("statistics", statistics);

        log.info("院校详情查询成功: schoolId={}, schoolName={}, majors={}, admissions={}",
                schoolId, school.getName(), majorIds.size(), admissionDataList.size());
        return result;
    }

    @Override
    public List<School> getSchoolsByIds(List<Integer> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<School> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(School::getId, schoolIds);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Long> getSchoolStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        // 查询总数
        long total = baseMapper.selectCount(Wrappers.lambdaQuery());
        statistics.put("total", total);
        
        // 查询启用的院校数
        long enabled = baseMapper.selectCount(
            Wrappers.<School>lambdaQuery().eq(School::getStatus, 1)
        );
        statistics.put("enabled", enabled);
        
        // 这里可以添加更多统计信息，如按省份、层次等分组统计
        
        return statistics;
    }

    @Override
    public boolean checkCodeExists(String code, Integer excludeId) {
        LambdaQueryWrapper<School> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(School::getCode, code);
        if (excludeId != null) {
            queryWrapper.ne(School::getId, excludeId);
        }
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public List<School> getSchoolsByLocation(Double minLng, Double maxLng, Double minLat, Double maxLat) {
        LambdaQueryWrapper<School> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(School::getStatus, 1); // 只查询启用的院校
        
        if (minLng != null && maxLng != null) {
            queryWrapper.between(School::getLongitude, minLng, maxLng);
        }
        if (minLat != null && maxLat != null) {
            queryWrapper.between(School::getLatitude, minLat, maxLat);
        }
        
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 构建地图查询条件
     */
    private LambdaQueryWrapper<School> buildMapQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<School> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(School::getStatus, 1); // 只查询启用的院校
        
        // 省份筛选
        if (params.containsKey("province") && params.get("province") != null) {
            queryWrapper.eq(School::getProvince, params.get("province"));
        }
        
        // 城市筛选
        if (params.containsKey("city") && params.get("city") != null) {
            queryWrapper.eq(School::getCity, params.get("city"));
        }
        
        // 院校层次筛选
        if (params.containsKey("level") && params.get("level") != null) {
            queryWrapper.eq(School::getLevel, params.get("level"));
        }
        
        // 院校类型筛选
        if (params.containsKey("type") && params.get("type") != null) {
            queryWrapper.eq(School::getType, params.get("type"));
        }
        
        // 隶属单位筛选
        if (params.containsKey("belong") && params.get("belong") != null) {
            queryWrapper.eq(School::getBelong, params.get("belong"));
        }
        
        // 经纬度范围筛选
        if (params.containsKey("minLng") && params.containsKey("maxLng") 
                && params.get("minLng") != null && params.get("maxLng") != null) {
            queryWrapper.between(School::getLongitude, params.get("minLng"), params.get("maxLng"));
        }
        
        if (params.containsKey("minLat") && params.containsKey("maxLat") 
                && params.get("minLat") != null && params.get("maxLat") != null) {
            queryWrapper.between(School::getLatitude, params.get("minLat"), params.get("maxLat"));
        }
        
        return queryWrapper;
    }

    /**
     * 构建搜索查询条件
     */
    private LambdaQueryWrapper<School> buildSearchQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<School> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(School::getStatus, 1); // 只查询启用的院校
        
        // 关键词搜索（院校名称或代码）
        if (params.containsKey("keyword") && params.get("keyword") != null) {
            String keyword = (String) params.get("keyword");
            queryWrapper.and(wrapper -> wrapper
                .like(School::getName, keyword)
                .or()
                .like(School::getCode, keyword)
            );
        }
        
        // 省份筛选
        if (params.containsKey("province") && params.get("province") != null) {
            queryWrapper.eq(School::getProvince, params.get("province"));
        }
        
        // 城市筛选
        if (params.containsKey("city") && params.get("city") != null) {
            queryWrapper.eq(School::getCity, params.get("city"));
        }
        
        // 院校层次筛选
        if (params.containsKey("level") && params.get("level") != null) {
            queryWrapper.eq(School::getLevel, params.get("level"));
        }
        
        // 院校类型筛选
        if (params.containsKey("type") && params.get("type") != null) {
            queryWrapper.eq(School::getType, params.get("type"));
        }
        
        // 隶属单位筛选
        if (params.containsKey("belong") && params.get("belong") != null) {
            queryWrapper.eq(School::getBelong, params.get("belong"));
        }
        
        return queryWrapper;
    }

    /**
     * 获取排序字段
     */
    private SFunction<School, ?> getSortField(String sortField) {
        switch (sortField) {
            case "name":
                return School::getName;
            case "level":
                return School::getLevel;
            case "province":
                return School::getProvince;
            case "city":
                return School::getCity;
            default:
                return School::getId;
        }
    }
}