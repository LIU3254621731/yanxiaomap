package com.yanxiaomap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yanxiaomap.entity.*;
import com.yanxiaomap.mapper.DisciplineMapper;
import com.yanxiaomap.mapper.MajorMapper;
import com.yanxiaomap.mapper.SchoolMajorMapper;
import com.yanxiaomap.mapper.SubjectCategoryMapper;
import com.yanxiaomap.service.AdmissionDataService;
import com.yanxiaomap.service.MajorService;
import com.yanxiaomap.service.SchoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专业服务实现类
 */
@Slf4j
@Service
public class MajorServiceImpl extends ServiceImpl<MajorMapper, Major> implements MajorService {

    @Autowired
    private DisciplineMapper disciplineMapper;

    @Autowired
    private SubjectCategoryMapper subjectCategoryMapper;

    @Autowired
    private SchoolMajorMapper schoolMajorMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AdmissionDataService admissionDataService;

    @Override
    public Page<Major> searchMajors(Map<String, Object> params) {
        LambdaQueryWrapper<Major> queryWrapper = buildSearchQueryWrapper(params);
        
        Integer page = (Integer) params.getOrDefault("page", 1);
        Integer size = (Integer) params.getOrDefault("size", 20);
        String sortField = (String) params.getOrDefault("sortField", "id");
        String sortOrder = (String) params.getOrDefault("sortOrder", "desc");
        
        // 构建分页对象
        Page<Major> pageObj = new Page<>(page, size);
        
        // 设置排序
        if ("asc".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc(getSortField(sortField));
        } else {
            queryWrapper.orderByDesc(getSortField(sortField));
        }
        
        return baseMapper.selectPage(pageObj, queryWrapper);
    }

    @Override
    public Map<String, Object> getMajorDetail(Integer majorId) {
        log.info("查询专业详情: majorId={}", majorId);
        Map<String, Object> result = new HashMap<>();
        
        if (majorId == null) {
            log.warn("查询专业详情失败: majorId不能为空");
            result.put("success", false);
            result.put("message", "专业ID不能为空");
            return result;
        }
        
        // 查询专业基本信息
        Major major = baseMapper.selectById(majorId);
        if (major == null) {
            log.warn("查询专业详情失败: 专业不存在 majorId={}", majorId);
            result.put("success", false);
            result.put("message", "专业不存在");
            return result;
        }
        
        // 构建专业信息
        Map<String, Object> majorInfo = new HashMap<>();
        majorInfo.put("id", major.getId());
        majorInfo.put("code", major.getCode());
        majorInfo.put("name", major.getName());
        majorInfo.put("alias", major.getAlias());
        majorInfo.put("categoryId", major.getCategoryId());
        majorInfo.put("disciplineId", major.getDisciplineId());
        majorInfo.put("degreeType", major.getDegreeType());
        majorInfo.put("duration", major.getDuration());
        majorInfo.put("description", major.getDescription());
        majorInfo.put("trainingObjective", major.getTrainingObjective());
        majorInfo.put("mainCourses", major.getMainCourses());
        majorInfo.put("employmentDirection", major.getEmploymentDirection());
        majorInfo.put("status", major.getStatus());
        majorInfo.put("createdAt", major.getCreatedAt());
        majorInfo.put("updatedAt", major.getUpdatedAt());
        
        result.put("success", true);
        result.put("major", majorInfo);
        result.put("message", "查询成功");

        // 查询关联的学科门类和一级学科
        if (major.getCategoryId() != null) {
            SubjectCategory category = subjectCategoryMapper.selectById(major.getCategoryId());
            result.put("category", category);
        }
        if (major.getDisciplineId() != null) {
            Discipline discipline = disciplineMapper.selectById(major.getDisciplineId());
            result.put("discipline", discipline);
        }

        // 查询开设该专业的院校列表
        LambdaQueryWrapper<SchoolMajor> smWrapper = Wrappers.lambdaQuery();
        smWrapper.eq(SchoolMajor::getMajorId, majorId);
        List<SchoolMajor> schoolMajors = schoolMajorMapper.selectList(smWrapper);
        List<Integer> schoolIds = schoolMajors.stream()
                .map(SchoolMajor::getSchoolId)
                .collect(Collectors.toList());
        if (!schoolIds.isEmpty()) {
            List<School> schools = schoolService.getSchoolsByIds(schoolIds);
            result.put("schools", schools);
            result.put("schoolCount", schools.size());
        } else {
            result.put("schools", new ArrayList<>());
            result.put("schoolCount", 0);
        }

        // 查询该专业的招生数据
        List<AdmissionData> admissionDataList = admissionDataService.getByMajorId(majorId);
        result.put("admissionData", admissionDataList);

        // 统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("schoolCount", result.get("schoolCount"));
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

        log.info("专业详情查询成功: majorId={}, majorName={}, schools={}, admissions={}",
                majorId, major.getName(), result.get("schoolCount"), admissionDataList.size());
        return result;
    }

    @Override
    public List<Major> getMajorsByDisciplineId(Integer disciplineId) {
        if (disciplineId == null) {
            return List.of();
        }
        LambdaQueryWrapper<Major> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Major::getDisciplineId, disciplineId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Major> getMajorsByCategoryId(Integer categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        LambdaQueryWrapper<Major> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Major::getCategoryId, categoryId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Major> getMajorsByIds(List<Integer> majorIds) {
        if (majorIds == null || majorIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Major> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(Major::getId, majorIds);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public boolean checkCodeExists(String code, Integer excludeId) {
        LambdaQueryWrapper<Major> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Major::getCode, code);
        if (excludeId != null) {
            queryWrapper.ne(Major::getId, excludeId);
        }
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public Map<String, Long> getMajorStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        // 查询总数
        long total = baseMapper.selectCount(Wrappers.lambdaQuery());
        statistics.put("total", total);
        
        // 查询启用状态的专业数
        long enabled = baseMapper.selectCount(
            Wrappers.<Major>lambdaQuery().eq(Major::getStatus, 1)
        );
        statistics.put("enabled", enabled);
        
        // 这里可以添加更多统计信息，如按学科门类、一级学科等分组统计
        
        return statistics;
    }

    @Override
    public List<Major> searchMajorsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Major> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Major::getStatus, 1);
        
        // 搜索专业名称或代码
        queryWrapper.and(wrapper -> wrapper
            .like(Major::getName, keyword)
            .or()
            .like(Major::getCode, keyword)
            .or()
            .like(Major::getAlias, keyword)
        );
        
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 构建搜索查询条件
     */
    private LambdaQueryWrapper<Major> buildSearchQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<Major> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Major::getStatus, 1); // 只查询启用的专业
        
        // 关键词搜索（专业名称、代码或别名）
        if (params.containsKey("keyword") && params.get("keyword") != null) {
            String keyword = (String) params.get("keyword");
            queryWrapper.and(wrapper -> wrapper
                .like(Major::getName, keyword)
                .or()
                .like(Major::getCode, keyword)
                .or()
                .like(Major::getAlias, keyword)
            );
        }
        
        // 学科门类筛选
        if (params.containsKey("categoryId") && params.get("categoryId") != null) {
            queryWrapper.eq(Major::getCategoryId, params.get("categoryId"));
        }
        
        // 一级学科筛选
        if (params.containsKey("disciplineId") && params.get("disciplineId") != null) {
            queryWrapper.eq(Major::getDisciplineId, params.get("disciplineId"));
        }
        
        // 学位类型筛选
        if (params.containsKey("degreeType") && params.get("degreeType") != null) {
            queryWrapper.eq(Major::getDegreeType, params.get("degreeType"));
        }
        
        // 学制筛选
        if (params.containsKey("duration") && params.get("duration") != null) {
            queryWrapper.eq(Major::getDuration, params.get("duration"));
        }
        
        return queryWrapper;
    }

    /**
     * 获取排序字段
     */
    private SFunction<Major, ?> getSortField(String sortField) {
        switch (sortField) {
            case "name":
                return Major::getName;
            case "code":
                return Major::getCode;
            case "categoryId":
                return Major::getCategoryId;
            case "disciplineId":
                return Major::getDisciplineId;
            default:
                return Major::getId;
        }
    }
}