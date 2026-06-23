package com.yanxiaomap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yanxiaomap.entity.AdmissionData;
import com.yanxiaomap.mapper.AdmissionDataMapper;
import com.yanxiaomap.service.AdmissionDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 招生录取数据服务实现类
 */
@Slf4j
@Service
public class AdmissionDataServiceImpl extends ServiceImpl<AdmissionDataMapper, AdmissionData> implements AdmissionDataService {

    @Override
    public Page<AdmissionData> searchAdmissionData(Map<String, Object> params) {
        LambdaQueryWrapper<AdmissionData> queryWrapper = buildSearchQueryWrapper(params);
        
        Integer page = (Integer) params.getOrDefault("page", 1);
        Integer size = (Integer) params.getOrDefault("size", 20);
        String sortField = (String) params.getOrDefault("sortField", "id");
        String sortOrder = (String) params.getOrDefault("sortOrder", "desc");
        
        // 构建分页对象
        Page<AdmissionData> pageObj = new Page<>(page, size);
        
        // 设置排序
        if ("asc".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc(getSortField(sortField));
        } else {
            queryWrapper.orderByDesc(getSortField(sortField));
        }
        
        return baseMapper.selectPage(pageObj, queryWrapper);
    }

    @Override
    public List<AdmissionData> getBySchoolIdAndMajorId(Integer schoolId, Integer majorId) {
        if (schoolId == null || majorId == null) {
            return List.of();
        }
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getSchoolId, schoolId);
        queryWrapper.eq(AdmissionData::getMajorId, majorId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<AdmissionData> getBySchoolId(Integer schoolId) {
        if (schoolId == null) {
            return List.of();
        }
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getSchoolId, schoolId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<AdmissionData> getByMajorId(Integer majorId) {
        if (majorId == null) {
            return List.of();
        }
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getMajorId, majorId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<AdmissionData> getByYear(Integer year) {
        if (year == null) {
            return List.of();
        }
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getYear, year);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Object> getSchoolAdmissionHistory(Integer schoolId, Integer startYear, Integer endYear) {
        Map<String, Object> result = new HashMap<>();
        
        if (schoolId == null) {
            return result;
        }
        
        // 构建查询条件
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getSchoolId, schoolId);
        
        // 年份范围筛选
        if (startYear != null) {
            queryWrapper.ge(AdmissionData::getYear, startYear);
        }
        if (endYear != null) {
            queryWrapper.le(AdmissionData::getYear, endYear);
        }
        
        // 查询数据
        List<AdmissionData> admissionDataList = baseMapper.selectList(queryWrapper);
        result.put("admissionData", admissionDataList);
        
        // 计算统计信息
        if (!admissionDataList.isEmpty()) {
            // 计算平均录取分数
            double avgScore = admissionDataList.stream()
                .mapToInt(AdmissionData::getAverageAdmissionScore)
                .average()
                .orElse(0.0);
            result.put("averageScore", avgScore);
            
            // 计算录取人数总和
            int totalEnrollment = admissionDataList.stream()
                .mapToInt(AdmissionData::getActualEnroll)
                .sum();
            result.put("totalEnrollment", totalEnrollment);
            
            // 计算最低录取分数
            int minScore = admissionDataList.stream()
                .mapToInt(AdmissionData::getAverageAdmissionScore)
                .min()
                .orElse(0);
            result.put("minimumScore", minScore);
            
            // 计算最高录取分数
            int maxScore = admissionDataList.stream()
                .mapToInt(AdmissionData::getAverageAdmissionScore)
                .max()
                .orElse(0);
            result.put("maximumScore", maxScore);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getMajorAdmissionHistory(Integer majorId, Integer startYear, Integer endYear) {
        Map<String, Object> result = new HashMap<>();
        
        if (majorId == null) {
            return result;
        }
        
        // 构建查询条件
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getMajorId, majorId);
        
        // 年份范围筛选
        if (startYear != null) {
            queryWrapper.ge(AdmissionData::getYear, startYear);
        }
        if (endYear != null) {
            queryWrapper.le(AdmissionData::getYear, endYear);
        }
        
        // 查询数据
        List<AdmissionData> admissionDataList = baseMapper.selectList(queryWrapper);
        result.put("admissionData", admissionDataList);
        
        // 计算统计信息
        if (!admissionDataList.isEmpty()) {
            // 计算平均录取分数
            double avgScore = admissionDataList.stream()
                .mapToInt(AdmissionData::getAverageAdmissionScore)
                .average()
                .orElse(0.0);
            result.put("averageScore", avgScore);
            
            // 计算录取人数总和
            int totalEnrollment = admissionDataList.stream()
                .mapToInt(AdmissionData::getActualEnroll)
                .sum();
            result.put("totalEnrollment", totalEnrollment);
            
            // 计算最低录取分数
            int minScore = admissionDataList.stream()
                .mapToInt(AdmissionData::getAverageAdmissionScore)
                .min()
                .orElse(0);
            result.put("minimumScore", minScore);
            
            // 计算最高录取分数
            int maxScore = admissionDataList.stream()
                .mapToInt(AdmissionData::getAverageAdmissionScore)
                .max()
                .orElse(0);
            result.put("maximumScore", maxScore);
        }
        
        return result;
    }

    @Override
    public List<AdmissionData> getAdmissionDataByIds(List<Integer> admissionIds) {
        if (admissionIds == null || admissionIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(AdmissionData::getId, admissionIds);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Long> getAdmissionStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        // 查询总数
        long total = baseMapper.selectCount(Wrappers.lambdaQuery());
        statistics.put("total", total);
        
        // 查询各年份的招生数据数量
        // 这里可以使用自定义SQL查询，暂时用简单的统计
        for (int year = 2020; year <= 2024; year++) {
            long count = baseMapper.selectCount(
                Wrappers.<AdmissionData>lambdaQuery().eq(AdmissionData::getYear, year)
            );
            statistics.put("year_" + year, count);
        }
        
        return statistics;
    }

    @Override
    public boolean checkDuplicate(Integer schoolId, Integer majorId, Integer year) {
        if (schoolId == null || majorId == null || year == null) {
            return false;
        }
        
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AdmissionData::getSchoolId, schoolId);
        queryWrapper.eq(AdmissionData::getMajorId, majorId);
        queryWrapper.eq(AdmissionData::getYear, year);
        
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 构建搜索查询条件
     */
    private LambdaQueryWrapper<AdmissionData> buildSearchQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<AdmissionData> queryWrapper = Wrappers.lambdaQuery();
        
        // 院校ID筛选
        if (params.containsKey("schoolId") && params.get("schoolId") != null) {
            queryWrapper.eq(AdmissionData::getSchoolId, params.get("schoolId"));
        }
        
        // 专业ID筛选
        if (params.containsKey("majorId") && params.get("majorId") != null) {
            queryWrapper.eq(AdmissionData::getMajorId, params.get("majorId"));
        }
        
        // 年份筛选
        if (params.containsKey("year") && params.get("year") != null) {
            queryWrapper.eq(AdmissionData::getYear, params.get("year"));
        }
        
        // 年份范围筛选
        if (params.containsKey("startYear") && params.get("startYear") != null) {
            queryWrapper.ge(AdmissionData::getYear, params.get("startYear"));
        }
        if (params.containsKey("endYear") && params.get("endYear") != null) {
            queryWrapper.le(AdmissionData::getYear, params.get("endYear"));
        }
        
        // 最低分数筛选
        if (params.containsKey("minScore") && params.get("minScore") != null) {
            queryWrapper.ge(AdmissionData::getAverageAdmissionScore, params.get("minScore"));
        }
        if (params.containsKey("maxScore") && params.get("maxScore") != null) {
            queryWrapper.le(AdmissionData::getAverageAdmissionScore, params.get("maxScore"));
        }
        
        return queryWrapper;
    }

    /**
     * 获取排序字段
     */
    private SFunction<AdmissionData, ?> getSortField(String sortField) {
        switch (sortField) {
            case "year":
                return AdmissionData::getYear;
            case "admissionScore":
                return AdmissionData::getAverageAdmissionScore;
            case "schoolId":
                return AdmissionData::getSchoolId;
            case "majorId":
                return AdmissionData::getMajorId;
            default:
                return AdmissionData::getId;
        }
    }
}