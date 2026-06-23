package com.yanxiaomap.controller;

import com.yanxiaomap.common.Result;
import com.yanxiaomap.entity.School;
import com.yanxiaomap.entity.Major;
import com.yanxiaomap.entity.AdmissionData;
import com.yanxiaomap.entity.Discipline;
import com.yanxiaomap.entity.SubjectCategory;
import com.yanxiaomap.mapper.DisciplineMapper;
import com.yanxiaomap.mapper.SubjectCategoryMapper;
import com.yanxiaomap.service.SchoolService;
import com.yanxiaomap.service.MajorService;
import com.yanxiaomap.service.AdmissionDataService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多校对比控制器
 * 处理院校、专业等多维度对比功能
 */
@Slf4j
@RestController
@RequestMapping("/api/compare")
@Api(tags = "多校对比接口")
public class CompareController {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private MajorService majorService;

    @Autowired
    private AdmissionDataService admissionDataService;

    @Autowired
    private DisciplineMapper disciplineMapper;

    @Autowired
    private SubjectCategoryMapper subjectCategoryMapper;

    private final List<Map<String, Object>> compareHistoryStore = new ArrayList<>();
    private long compareHistoryIdCounter = 1;

    /**
     * 多院校对比
     * 支持最多5所院校同时对比
     */
    @PostMapping("/schools")
    @ApiOperation(value = "多院校对比", notes = "对比多所院校的基本信息、专业设置、招生数据等")
    public Result<Map<String, Object>> compareSchools(
            @ApiParam(value = "对比请求参数，包含schoolIds字段", required = true) @RequestBody Map<String, Object> params
    ) {
        log.info("多院校对比请求: params={}", params);

        try {
            // 提取院校ID列表
            List<Integer> schoolIds = new ArrayList<>();
            if (params.containsKey("schoolIds")) {
                Object schoolIdsObj = params.get("schoolIds");
                if (schoolIdsObj instanceof List) {
                    for (Object id : (List<?>) schoolIdsObj) {
                        if (id instanceof Integer) {
                            schoolIds.add((Integer) id);
                        } else if (id instanceof Number) {
                            schoolIds.add(((Number) id).intValue());
                        }
                    }
                }
            }

            // 验证参数
            if (schoolIds.isEmpty()) {
                return Result.error("请选择要对比的院校");
            }
            if (schoolIds.size() > 5) {
                return Result.error("最多支持5所院校同时对比");
            }

            // 查询院校信息
            List<School> schools = schoolService.getSchoolsByIds(schoolIds);
            if (schools.isEmpty()) {
                return Result.error("未找到对应的院校信息");
            }

            // 构建对比结果
            Map<String, Object> result = new HashMap<>();
            result.put("schools", schools);
            
            // 构建对比表格
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("basicInfo", compareSchoolBasicInfo(schools));
            comparison.put("locationInfo", compareSchoolLocationInfo(schools));
            comparison.put("levelInfo", compareSchoolLevelInfo(schools));
            
            result.put("comparison", comparison);
            
            // 生成亮点（差异点）
            List<String> highlights = generateSchoolComparisonHighlights(schools);
            result.put("highlights", highlights);
            
            return Result.success("院校对比成功", result);
        } catch (Exception e) {
            log.error("多院校对比失败", e);
            return Result.error("院校对比失败: " + e.getMessage());
        }
    }

    /**
     * 多专业对比
     * 支持最多5个专业同时对比
     */
    @PostMapping("/majors")
    @ApiOperation(value = "多专业对比", notes = "对比多个专业的基本信息、培养方案、招生数据等")
    public Result<Map<String, Object>> compareMajors(
            @ApiParam(value = "专业ID列表，最多5个", required = true) @RequestBody List<Integer> majorIds
    ) {
        log.info("多专业对比请求: majorIds={}", majorIds);

        try {
            // 验证参数
            if (majorIds == null || majorIds.isEmpty()) {
                return Result.error("请选择要对比的专业");
            }
            if (majorIds.size() > 5) {
                return Result.error("最多支持5个专业同时对比");
            }

            // 查询专业信息
            List<Major> majors = majorService.getMajorsByIds(majorIds);
            if (majors.isEmpty()) {
                return Result.error("未找到对应的专业信息");
            }

            // 构建对比结果
            Map<String, Object> result = new HashMap<>();
            result.put("majors", majors);
            
            // 构建对比表格
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("basicInfo", compareMajorBasicInfo(majors));
            comparison.put("educationInfo", compareMajorEducationInfo(majors));
            
            result.put("comparison", comparison);
            
            // 生成亮点（差异点）
            List<String> highlights = generateMajorComparisonHighlights(majors);
            result.put("highlights", highlights);
            
            return Result.success("专业对比成功", result);
        } catch (Exception e) {
            log.error("多专业对比失败", e);
            return Result.error("专业对比失败: " + e.getMessage());
        }
    }

    /**
     * 院校-专业组合对比
     * 对比不同院校的相同专业
     */
    @PostMapping("/school-major")
    @ApiOperation(value = "院校-专业组合对比", notes = "对比不同院校的相同专业的招生数据、录取要求等")
    public Result<Map<String, Object>> compareSchoolMajorCombinations(
            @ApiParam(value = "对比组合列表，每个组合包含院校ID和专业ID", required = true) 
            @RequestBody List<Map<String, Integer>> combinations
    ) {
        log.info("院校-专业组合对比请求: combinations={}", combinations);

        try {
            // 验证参数
            if (combinations == null || combinations.isEmpty()) {
                return Result.error("请选择要对比的院校-专业组合");
            }
            if (combinations.size() > 5) {
                return Result.error("最多支持5个组合同时对比");
            }

            // 验证组合格式
            for (Map<String, Integer> combination : combinations) {
                if (!combination.containsKey("schoolId") || !combination.containsKey("majorId")) {
                    return Result.error("每个组合必须包含schoolId和majorId字段");
                }
                if (combination.get("schoolId") == null || combination.get("majorId") == null) {
                    return Result.error("schoolId和majorId不能为空");
                }
            }

            // 查询组合详细信息
            List<Map<String, Object>> combinationDetails = new ArrayList<>();
            Map<Integer, School> schoolCache = new HashMap<>();
            Map<Integer, Major> majorCache = new HashMap<>();
            
            for (Map<String, Integer> combination : combinations) {
                Integer schoolId = combination.get("schoolId");
                Integer majorId = combination.get("majorId");
                
                // 查询院校信息（使用缓存避免重复查询）
                School school = schoolCache.get(schoolId);
                if (school == null) {
                    school = schoolService.getById(schoolId);
                    if (school == null) {
                        return Result.error("未找到院校ID为" + schoolId + "的院校信息");
                    }
                    schoolCache.put(schoolId, school);
                }
                
                // 查询专业信息（使用缓存避免重复查询）
                Major major = majorCache.get(majorId);
                if (major == null) {
                    major = majorService.getById(majorId);
                    if (major == null) {
                        return Result.error("未找到专业ID为" + majorId + "的专业信息");
                    }
                    majorCache.put(majorId, major);
                }
                
                // 查询该组合的招生数据（获取最新年份的数据）
                List<AdmissionData> admissionDataList = admissionDataService.getBySchoolIdAndMajorId(schoolId, majorId);
                AdmissionData latestAdmissionData = null;
                if (admissionDataList != null && !admissionDataList.isEmpty()) {
                    // 按年份降序排序，获取最新年份的数据
                    admissionDataList.sort((a, b) -> b.getYear() - a.getYear());
                    latestAdmissionData = admissionDataList.get(0);
                }
                
                // 构建组合详情
                Map<String, Object> detail = new HashMap<>();
                detail.put("schoolId", schoolId);
                detail.put("schoolName", school.getName());
                detail.put("majorId", majorId);
                detail.put("majorName", major.getName());
                detail.put("admissionData", latestAdmissionData);
                detail.put("admissionDataList", admissionDataList);
                
                combinationDetails.add(detail);
            }
            
            // 构建对比结果
            Map<String, Object> result = new HashMap<>();
            result.put("combinations", combinationDetails);
            
            // 构建对比表格
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("basicInfo", compareCombinationBasicInfo(combinationDetails));
            comparison.put("admissionInfo", compareCombinationAdmissionInfo(combinationDetails));
            comparison.put("scoreInfo", compareCombinationScoreInfo(combinationDetails));
            
            result.put("comparison", comparison);
            
            // 生成对比亮点
            List<String> highlights = generateCombinationHighlights(combinationDetails);
            result.put("highlights", highlights);
            
            return Result.success("院校-专业组合对比成功", result);
        } catch (Exception e) {
            log.error("院校-专业组合对比失败", e);
            return Result.error("院校-专业组合对比失败: " + e.getMessage());
        }
    }

    /**
     * 招生数据对比
     * 对比不同年份、不同院校、不同专业的招生数据
     */
    @PostMapping("/admission")
    @ApiOperation(value = "招生数据对比", notes = "对比不同年份、不同院校、不同专业的招生数据")
    public Result<Map<String, Object>> compareAdmissionData(
            @ApiParam(value = "对比条件", required = true) @RequestBody Map<String, Object> conditions
    ) {
        log.info("招生数据对比请求: conditions={}", conditions);

        try {
            // 验证参数
            if (conditions == null || conditions.isEmpty()) {
                return Result.error("请提供对比条件");
            }
            
            // 提取条件参数
            List<Integer> schoolIds = null;
            if (conditions.containsKey("schoolIds")) {
                Object schoolIdsObj = conditions.get("schoolIds");
                if (schoolIdsObj instanceof List) {
                    schoolIds = (List<Integer>) schoolIdsObj;
                }
            }
            
            List<Integer> majorIds = null;
            if (conditions.containsKey("majorIds")) {
                Object majorIdsObj = conditions.get("majorIds");
                if (majorIdsObj instanceof List) {
                    majorIds = (List<Integer>) majorIdsObj;
                }
            }
            
            Integer startYear = null;
            Integer endYear = null;
            if (conditions.containsKey("startYear")) {
                startYear = (Integer) conditions.get("startYear");
            }
            if (conditions.containsKey("endYear")) {
                endYear = (Integer) conditions.get("endYear");
            }
            
            // 构建查询参数
            Map<String, Object> queryParams = new HashMap<>();
            if (schoolIds != null && !schoolIds.isEmpty()) {
                queryParams.put("schoolIds", schoolIds);
            }
            if (majorIds != null && !majorIds.isEmpty()) {
                queryParams.put("majorIds", majorIds);
            }
            if (startYear != null) {
                queryParams.put("startYear", startYear);
            }
            if (endYear != null) {
                queryParams.put("endYear", endYear);
            }
            
            // 查询招生数据
            Page<AdmissionData> admissionPage = admissionDataService.searchAdmissionData(queryParams);
            List<AdmissionData> admissionDataList = admissionPage.getRecords();
            
            if (admissionDataList.isEmpty()) {
                return Result.error("未找到符合条件的招生数据");
            }
            
            // 构建对比结果
            Map<String, Object> result = new HashMap<>();
            result.put("data", admissionDataList);
            
            // 计算统计数据
            Map<String, Object> statistics = calculateAdmissionStatistics(admissionDataList);
            result.put("statistics", statistics);
            
            // 分析趋势
            List<Map<String, Object>> trends = analyzeAdmissionTrends(admissionDataList);
            result.put("trends", trends);
            
            // 生成对比亮点
            List<String> highlights = generateAdmissionComparisonHighlights(admissionDataList);
            result.put("highlights", highlights);
            
            return Result.success("招生数据对比成功", result);
        } catch (Exception e) {
            log.error("招生数据对比失败", e);
            return Result.error("招生数据对比失败: " + e.getMessage());
        }
    }

    /**
     * 获取对比历史记录
     */
    @GetMapping("/history")
    @ApiOperation(value = "获取对比历史记录", notes = "获取用户的对比历史记录")
    public Result<List<Map<String, Object>>> getCompareHistory(
            @ApiParam(value = "用户ID（待用户系统完成后使用）") @RequestParam(required = false) String userId,
            @ApiParam(value = "历史记录类型：schools/majors/admission") @RequestParam(required = false) String type,
            @ApiParam(value = "返回记录数量", defaultValue = "10") @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("获取对比历史记录请求: userId={}, type={}, limit={}", userId, type, limit);

        List<Map<String, Object>> history;
        if (type != null && !type.isEmpty()) {
            history = compareHistoryStore.stream()
                    .filter(item -> type.equals(item.get("type")))
                    .sorted((a, b) -> {
                        Long aTime = a.get("createdAt") instanceof Number ? ((Number) a.get("createdAt")).longValue() : 0;
                        Long bTime = b.get("createdAt") instanceof Number ? ((Number) b.get("createdAt")).longValue() : 0;
                        return bTime.compareTo(aTime);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            history = compareHistoryStore.stream()
                    .sorted((a, b) -> {
                        Long aTime = a.get("createdAt") instanceof Number ? ((Number) a.get("createdAt")).longValue() : 0;
                        Long bTime = b.get("createdAt") instanceof Number ? ((Number) b.get("createdAt")).longValue() : 0;
                        return bTime.compareTo(aTime);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return Result.success("获取对比历史记录成功", history);
    }

    /**
     * 保存对比结果
     */
    @PostMapping("/save")
    @ApiOperation(value = "保存对比结果", notes = "保存当前的对比结果到历史记录")
    public Result<Map<String, Object>> saveCompareResult(
            @ApiParam(value = "对比结果数据", required = true) @RequestBody Map<String, Object> compareData
    ) {
        log.info("保存对比结果请求: compareData={}", compareData.keySet());

        Map<String, Object> record = new HashMap<>(compareData);
        record.put("id", compareHistoryIdCounter++);
        record.put("createdAt", System.currentTimeMillis());

        compareHistoryStore.add(record);

        log.info("对比结果保存成功, 当前历史记录数: {}", compareHistoryStore.size());
        return Result.success("对比结果保存成功", record);
    }

    /**
     * 对比院校基本信息
     */
    private Map<String, List<Object>> compareSchoolBasicInfo(List<School> schools) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 院校名称
        List<Object> names = new ArrayList<>();
        for (School school : schools) {
            names.add(school.getName());
        }
        comparison.put("院校名称", names);
        
        // 院校代码
        List<Object> codes = new ArrayList<>();
        for (School school : schools) {
            codes.add(school.getCode());
        }
        comparison.put("院校代码", codes);
        
        // 院校类型
        List<Object> types = new ArrayList<>();
        for (School school : schools) {
            types.add(school.getType());
        }
        comparison.put("院校类型", types);
        
        // 院校层次
        List<Object> levels = new ArrayList<>();
        for (School school : schools) {
            levels.add(school.getLevel());
        }
        comparison.put("院校层次", levels);
        
        // 隶属单位
        List<Object> belongs = new ArrayList<>();
        for (School school : schools) {
            belongs.add(school.getBelong());
        }
        comparison.put("隶属单位", belongs);
        
        // 官网
        List<Object> websites = new ArrayList<>();
        for (School school : schools) {
            websites.add(school.getWebsite());
        }
        comparison.put("官方网站", websites);
        
        return comparison;
    }

    /**
     * 对比院校地理位置信息
     */
    private Map<String, List<Object>> compareSchoolLocationInfo(List<School> schools) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 省份
        List<Object> provinces = new ArrayList<>();
        for (School school : schools) {
            provinces.add(school.getProvince());
        }
        comparison.put("所在省份", provinces);
        
        // 城市
        List<Object> cities = new ArrayList<>();
        for (School school : schools) {
            cities.add(school.getCity());
        }
        comparison.put("所在城市", cities);
        
        // 经度
        List<Object> longitudes = new ArrayList<>();
        for (School school : schools) {
            longitudes.add(school.getLongitude());
        }
        comparison.put("经度", longitudes);
        
        // 纬度
        List<Object> latitudes = new ArrayList<>();
        for (School school : schools) {
            latitudes.add(school.getLatitude());
        }
        comparison.put("纬度", latitudes);
        
        return comparison;
    }

    /**
     * 对比院校层次信息
     */
    private Map<String, List<Object>> compareSchoolLevelInfo(List<School> schools) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 是否为985
        List<Object> is985 = new ArrayList<>();
        for (School school : schools) {
            is985.add("985".equals(school.getLevel()));
        }
        comparison.put("是否为985", is985);
        
        // 是否为211
        List<Object> is211 = new ArrayList<>();
        for (School school : schools) {
            is211.add("211".equals(school.getLevel()));
        }
        comparison.put("是否为211", is211);
        
        // 是否为双一流
        List<Object> isDoubleFirstClass = new ArrayList<>();
        for (School school : schools) {
            isDoubleFirstClass.add("双一流".equals(school.getLevel()));
        }
        comparison.put("是否为双一流", isDoubleFirstClass);
        
        return comparison;
    }

    /**
     * 生成院校对比亮点（差异点）
     */
    private List<String> generateSchoolComparisonHighlights(List<School> schools) {
        List<String> highlights = new ArrayList<>();
        
        if (schools.size() < 2) {
            return highlights;
        }
        
        // 检查层次差异
        Set<String> levels = new HashSet<>();
        for (School school : schools) {
            levels.add(school.getLevel());
        }
        if (levels.size() > 1) {
            highlights.add("院校层次多样，包含" + String.join("、", levels) + "等不同层次院校");
        }
        
        // 检查省份差异
        Set<String> provinces = new HashSet<>();
        for (School school : schools) {
            provinces.add(school.getProvince());
        }
        if (provinces.size() > 1) {
            highlights.add("院校分布在不同省份：" + String.join("、", provinces));
        }
        
        // 检查类型差异
        Set<String> types = new HashSet<>();
        for (School school : schools) {
            types.add(school.getType());
        }
        if (types.size() > 1) {
            highlights.add("院校类型多样，包含" + String.join("、", types) + "等不同类型院校");
        }
        
        return highlights;
    }

    /**
     * 对比专业基本信息
     */
    private Map<String, List<Object>> compareMajorBasicInfo(List<Major> majors) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 专业名称
        List<Object> names = new ArrayList<>();
        for (Major major : majors) {
            names.add(major.getName());
        }
        comparison.put("专业名称", names);
        
        // 专业代码
        List<Object> codes = new ArrayList<>();
        for (Major major : majors) {
            codes.add(major.getCode());
        }
        comparison.put("专业代码", codes);
        
        // 学位类型
        List<Object> degreeTypes = new ArrayList<>();
        for (Major major : majors) {
            degreeTypes.add(major.getDegreeType());
        }
        comparison.put("学位类型", degreeTypes);
        
        // 学制
        List<Object> durations = new ArrayList<>();
        for (Major major : majors) {
            durations.add(major.getDuration() + "年");
        }
        comparison.put("学制", durations);
        
        // 是否全日制
        List<Object> fullTimes = new ArrayList<>();
        for (Major major : majors) {
            fullTimes.add(major.getFullTime() == 1 ? "全日制" : "非全日制");
        }
        comparison.put("培养方式", fullTimes);
        
        return comparison;
    }

    /**
     * 对比专业教育信息
     */
    private Map<String, List<Object>> compareMajorEducationInfo(List<Major> majors) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 学科门类（查询真实名称）
        List<Object> categories = new ArrayList<>();
        for (Major major : majors) {
            String categoryName = "未知";
            if (major.getCategoryId() != null) {
                SubjectCategory category = subjectCategoryMapper.selectById(major.getCategoryId());
                if (category != null) {
                    categoryName = category.getName();
                }
            }
            categories.add(categoryName);
        }
        comparison.put("学科门类", categories);
        
        // 一级学科（查询真实名称）
        List<Object> disciplines = new ArrayList<>();
        for (Major major : majors) {
            String disciplineName = "未知";
            if (major.getDisciplineId() != null) {
                Discipline discipline = disciplineMapper.selectById(major.getDisciplineId());
                if (discipline != null) {
                    disciplineName = discipline.getName();
                }
            }
            disciplines.add(disciplineName);
        }
        comparison.put("一级学科", disciplines);
        
        return comparison;
    }

    /**
     * 生成专业对比亮点（差异点）
     */
    private List<String> generateMajorComparisonHighlights(List<Major> majors) {
        List<String> highlights = new ArrayList<>();
        
        if (majors.size() < 2) {
            return highlights;
        }
        
        // 检查学位类型差异
        Set<String> degreeTypes = new HashSet<>();
        for (Major major : majors) {
            degreeTypes.add(major.getDegreeType());
        }
        if (degreeTypes.size() > 1) {
            highlights.add("学位类型多样，包含" + String.join("、", degreeTypes) + "等不同学位类型");
        }
        
        // 检查学制差异
        Set<Integer> durations = new HashSet<>();
        for (Major major : majors) {
            durations.add(major.getDuration());
        }
        if (durations.size() > 1) {
            StringBuilder durationStr = new StringBuilder();
            for (Integer duration : durations) {
                durationStr.append(duration).append("年、");
            }
            if (durationStr.length() > 0) {
                durationStr.setLength(durationStr.length() - 1); // 移除最后一个"、"
            }
            highlights.add("学制不同，包含" + durationStr.toString() + "等多种学制");
        }
        
        // 检查培养方式差异
        boolean hasFullTime = false;
        boolean hasPartTime = false;
        for (Major major : majors) {
            if (major.getFullTime() == 1) {
                hasFullTime = true;
            } else {
                hasPartTime = true;
            }
        }
        if (hasFullTime && hasPartTime) {
            highlights.add("包含全日制和非全日制两种培养方式");
        }
        
        return highlights;
    }

    /**
     * 对比组合基本信息
     */
    private Map<String, List<Object>> compareCombinationBasicInfo(List<Map<String, Object>> combinationDetails) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 院校名称
        List<Object> schoolNames = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            schoolNames.add(detail.get("schoolName"));
        }
        comparison.put("院校名称", schoolNames);
        
        // 专业名称
        List<Object> majorNames = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            majorNames.add(detail.get("majorName"));
        }
        comparison.put("专业名称", majorNames);
        
        // 院校-专业组合
        List<Object> combinations = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            combinations.add(detail.get("schoolName") + " - " + detail.get("majorName"));
        }
        comparison.put("院校-专业组合", combinations);
        
        return comparison;
    }

    /**
     * 对比组合招生信息
     */
    private Map<String, List<Object>> compareCombinationAdmissionInfo(List<Map<String, Object>> combinationDetails) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 最新招生年份
        List<Object> years = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            years.add(admissionData != null ? admissionData.getYear() : "无数据");
        }
        comparison.put("招生年份", years);
        
        // 计划招生人数
        List<Object> planEnrolls = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            planEnrolls.add(admissionData != null ? admissionData.getPlanEnroll() : "无数据");
        }
        comparison.put("计划招生人数", planEnrolls);
        
        // 实际录取人数
        List<Object> actualEnrolls = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            actualEnrolls.add(admissionData != null ? admissionData.getActualEnroll() : "无数据");
        }
        comparison.put("实际录取人数", actualEnrolls);
        
        // 推免人数
        List<Object> recommendedCounts = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            recommendedCounts.add(admissionData != null ? admissionData.getRecommendedCount() : "无数据");
        }
        comparison.put("推免人数", recommendedCounts);
        
        // 报录比
        List<Object> admissionRatios = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            admissionRatios.add(admissionData != null ? admissionData.getAdmissionRatio() : "无数据");
        }
        comparison.put("报录比", admissionRatios);
        
        return comparison;
    }

    /**
     * 对比组合分数信息
     */
    private Map<String, List<Object>> compareCombinationScoreInfo(List<Map<String, Object>> combinationDetails) {
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // 复试总分线
        List<Object> retestTotalScores = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            retestTotalScores.add(admissionData != null ? admissionData.getRetestTotalScore() : "无数据");
        }
        comparison.put("复试总分线", retestTotalScores);
        
        // 单科线
        List<Object> singleSubjectScores = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            singleSubjectScores.add(admissionData != null ? admissionData.getSingleSubjectScore() : "无数据");
        }
        comparison.put("单科线", singleSubjectScores);
        
        // 录取平均分
        List<Object> averageAdmissionScores = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            averageAdmissionScores.add(admissionData != null ? admissionData.getAverageAdmissionScore() : "无数据");
        }
        comparison.put("录取平均分", averageAdmissionScores);
        
        return comparison;
    }

    /**
     * 生成组合对比亮点（差异点）
     */
    private List<String> generateCombinationHighlights(List<Map<String, Object>> combinationDetails) {
        List<String> highlights = new ArrayList<>();
        
        if (combinationDetails.size() < 2) {
            return highlights;
        }
        
        // 检查院校差异
        Set<String> schools = new HashSet<>();
        for (Map<String, Object> detail : combinationDetails) {
            schools.add((String) detail.get("schoolName"));
        }
        if (schools.size() > 1) {
            highlights.add("对比涉及" + schools.size() + "所不同院校：" + String.join("、", schools));
        }
        
        // 检查专业差异
        Set<String> majors = new HashSet<>();
        for (Map<String, Object> detail : combinationDetails) {
            majors.add((String) detail.get("majorName"));
        }
        if (majors.size() > 1) {
            highlights.add("对比涉及" + majors.size() + "个不同专业：" + String.join("、", majors));
        } else if (majors.size() == 1) {
            highlights.add("所有组合均为" + majors.iterator().next() + "专业，便于对比不同院校的同一专业");
        }
        
        // 检查招生数据完整性
        int hasDataCount = 0;
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            if (admissionData != null) {
                hasDataCount++;
            }
        }
        if (hasDataCount == 0) {
            highlights.add("注意：所有组合均无招生数据");
        } else if (hasDataCount < combinationDetails.size()) {
            highlights.add("注意：部分组合缺少招生数据，仅" + hasDataCount + "/" + combinationDetails.size() + "个组合有数据");
        }
        
        // 检查报录比差异
        List<BigDecimal> ratios = new ArrayList<>();
        for (Map<String, Object> detail : combinationDetails) {
            AdmissionData admissionData = (AdmissionData) detail.get("admissionData");
            if (admissionData != null && admissionData.getAdmissionRatio() != null) {
                ratios.add(admissionData.getAdmissionRatio());
            }
        }
        if (ratios.size() >= 2) {
            BigDecimal minRatio = ratios.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxRatio = ratios.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (minRatio.compareTo(maxRatio) != 0) {
                highlights.add("报录比差异明显，从" + minRatio + "到" + maxRatio);
            }
        }
        
        return highlights;
    }

    /**
     * 计算招生数据统计信息
     */
    private Map<String, Object> calculateAdmissionStatistics(List<AdmissionData> admissionDataList) {
        Map<String, Object> statistics = new HashMap<>();
        
        if (admissionDataList.isEmpty()) {
            return statistics;
        }
        
        // 基本统计
        statistics.put("totalRecords", admissionDataList.size());
        
        // 院校数量
        Set<Integer> schoolIds = new HashSet<>();
        for (AdmissionData data : admissionDataList) {
            schoolIds.add(data.getSchoolId());
        }
        statistics.put("schoolCount", schoolIds.size());
        
        // 专业数量
        Set<Integer> majorIds = new HashSet<>();
        for (AdmissionData data : admissionDataList) {
            majorIds.add(data.getMajorId());
        }
        statistics.put("majorCount", majorIds.size());
        
        // 年份范围
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        for (AdmissionData data : admissionDataList) {
            int year = data.getYear();
            if (year < minYear) minYear = year;
            if (year > maxYear) maxYear = year;
        }
        statistics.put("yearRange", minYear + "-" + maxYear);
        statistics.put("minYear", minYear);
        statistics.put("maxYear", maxYear);
        
        // 招生人数统计
        int totalPlanEnroll = 0;
        int totalActualEnroll = 0;
        int totalRecommendedCount = 0;
        for (AdmissionData data : admissionDataList) {
            if (data.getPlanEnroll() != null) totalPlanEnroll += data.getPlanEnroll();
            if (data.getActualEnroll() != null) totalActualEnroll += data.getActualEnroll();
            if (data.getRecommendedCount() != null) totalRecommendedCount += data.getRecommendedCount();
        }
        statistics.put("totalPlanEnroll", totalPlanEnroll);
        statistics.put("totalActualEnroll", totalActualEnroll);
        statistics.put("totalRecommendedCount", totalRecommendedCount);
        
        // 平均报录比
        BigDecimal totalRatio = BigDecimal.ZERO;
        int ratioCount = 0;
        for (AdmissionData data : admissionDataList) {
            if (data.getAdmissionRatio() != null) {
                totalRatio = totalRatio.add(data.getAdmissionRatio());
                ratioCount++;
            }
        }
        if (ratioCount > 0) {
            BigDecimal avgRatio = totalRatio.divide(BigDecimal.valueOf(ratioCount), 2, BigDecimal.ROUND_HALF_UP);
            statistics.put("averageAdmissionRatio", avgRatio);
        }
        
        // 分数统计
        List<Integer> retestScores = new ArrayList<>();
        List<Integer> averageScores = new ArrayList<>();
        for (AdmissionData data : admissionDataList) {
            if (data.getRetestTotalScore() != null) retestScores.add(data.getRetestTotalScore());
            if (data.getAverageAdmissionScore() != null) averageScores.add(data.getAverageAdmissionScore());
        }
        
        if (!retestScores.isEmpty()) {
            statistics.put("minRetestScore", Collections.min(retestScores));
            statistics.put("maxRetestScore", Collections.max(retestScores));
            statistics.put("avgRetestScore", retestScores.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
        
        if (!averageScores.isEmpty()) {
            statistics.put("minAverageScore", Collections.min(averageScores));
            statistics.put("maxAverageScore", Collections.max(averageScores));
            statistics.put("avgAverageScore", averageScores.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
        
        return statistics;
    }

    /**
     * 分析招生数据趋势
     */
    private List<Map<String, Object>> analyzeAdmissionTrends(List<AdmissionData> admissionDataList) {
        List<Map<String, Object>> trends = new ArrayList<>();
        
        if (admissionDataList.isEmpty()) {
            return trends;
        }
        
        // 按年份分组
        Map<Integer, List<AdmissionData>> dataByYear = new HashMap<>();
        for (AdmissionData data : admissionDataList) {
            dataByYear.computeIfAbsent(data.getYear(), k -> new ArrayList<>()).add(data);
        }
        
        // 按年份排序
        List<Integer> years = new ArrayList<>(dataByYear.keySet());
        Collections.sort(years);
        
        for (Integer year : years) {
            List<AdmissionData> yearData = dataByYear.get(year);
            
            // 计算年度统计
            Map<String, Object> yearTrend = new HashMap<>();
            yearTrend.put("year", year);
            yearTrend.put("recordCount", yearData.size());
            
            // 招生人数趋势
            int yearPlanEnroll = yearData.stream().mapToInt(data -> data.getPlanEnroll() != null ? data.getPlanEnroll() : 0).sum();
            int yearActualEnroll = yearData.stream().mapToInt(data -> data.getActualEnroll() != null ? data.getActualEnroll() : 0).sum();
            yearTrend.put("planEnroll", yearPlanEnroll);
            yearTrend.put("actualEnroll", yearActualEnroll);
            
            // 报录比趋势
            BigDecimal yearTotalRatio = BigDecimal.ZERO;
            int yearRatioCount = 0;
            for (AdmissionData data : yearData) {
                if (data.getAdmissionRatio() != null) {
                    yearTotalRatio = yearTotalRatio.add(data.getAdmissionRatio());
                    yearRatioCount++;
                }
            }
            if (yearRatioCount > 0) {
                BigDecimal yearAvgRatio = yearTotalRatio.divide(BigDecimal.valueOf(yearRatioCount), 2, BigDecimal.ROUND_HALF_UP);
                yearTrend.put("averageAdmissionRatio", yearAvgRatio);
            }
            
            // 分数趋势
            List<Integer> yearRetestScores = new ArrayList<>();
            List<Integer> yearAverageScores = new ArrayList<>();
            for (AdmissionData data : yearData) {
                if (data.getRetestTotalScore() != null) yearRetestScores.add(data.getRetestTotalScore());
                if (data.getAverageAdmissionScore() != null) yearAverageScores.add(data.getAverageAdmissionScore());
            }
            
            if (!yearRetestScores.isEmpty()) {
                yearTrend.put("avgRetestScore", yearRetestScores.stream().mapToInt(Integer::intValue).average().orElse(0));
            }
            if (!yearAverageScores.isEmpty()) {
                yearTrend.put("avgAverageScore", yearAverageScores.stream().mapToInt(Integer::intValue).average().orElse(0));
            }
            
            trends.add(yearTrend);
        }
        
        return trends;
    }

    /**
     * 生成招生数据对比亮点（差异点）
     */
    private List<String> generateAdmissionComparisonHighlights(List<AdmissionData> admissionDataList) {
        List<String> highlights = new ArrayList<>();
        
        if (admissionDataList.size() < 2) {
            return highlights;
        }
        
        // 检查年份跨度
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        for (AdmissionData data : admissionDataList) {
            int year = data.getYear();
            if (year < minYear) minYear = year;
            if (year > maxYear) maxYear = year;
        }
        int yearSpan = maxYear - minYear;
        if (yearSpan > 0) {
            highlights.add("数据覆盖" + yearSpan + "年时间跨度（" + minYear + "-" + maxYear + "）");
        }
        
        // 检查院校多样性
        Set<Integer> schoolIds = new HashSet<>();
        for (AdmissionData data : admissionDataList) {
            schoolIds.add(data.getSchoolId());
        }
        if (schoolIds.size() > 1) {
            highlights.add("涉及" + schoolIds.size() + "所不同院校");
        }
        
        // 检查专业多样性
        Set<Integer> majorIds = new HashSet<>();
        for (AdmissionData data : admissionDataList) {
            majorIds.add(data.getMajorId());
        }
        if (majorIds.size() > 1) {
            highlights.add("涉及" + majorIds.size() + "个不同专业");
        }
        
        // 检查招生规模变化
        Map<Integer, Integer> enrollByYear = new HashMap<>();
        for (AdmissionData data : admissionDataList) {
            int year = data.getYear();
            int enroll = data.getPlanEnroll() != null ? data.getPlanEnroll() : 0;
            enrollByYear.put(year, enrollByYear.getOrDefault(year, 0) + enroll);
        }
        
        if (enrollByYear.size() >= 2) {
            List<Integer> sortedYears = new ArrayList<>(enrollByYear.keySet());
            Collections.sort(sortedYears);
            
            int firstYear = sortedYears.get(0);
            int lastYear = sortedYears.get(sortedYears.size() - 1);
            int firstEnroll = enrollByYear.get(firstYear);
            int lastEnroll = enrollByYear.get(lastYear);
            
            if (lastEnroll > firstEnroll) {
                double growthRate = ((double) (lastEnroll - firstEnroll) / firstEnroll) * 100;
                highlights.add(String.format("招生规模呈增长趋势，从%d年的%d人增长到%d年的%d人，增长率为%.1f%%", 
                    firstYear, firstEnroll, lastYear, lastEnroll, growthRate));
            } else if (lastEnroll < firstEnroll) {
                double declineRate = ((double) (firstEnroll - lastEnroll) / firstEnroll) * 100;
                highlights.add(String.format("招生规模呈下降趋势，从%d年的%d人减少到%d年的%d人，下降率为%.1f%%", 
                    firstYear, firstEnroll, lastYear, lastEnroll, declineRate));
            }
        }
        
        // 检查报录比差异
        List<BigDecimal> ratios = new ArrayList<>();
        for (AdmissionData data : admissionDataList) {
            if (data.getAdmissionRatio() != null) {
                ratios.add(data.getAdmissionRatio());
            }
        }
        if (ratios.size() >= 2) {
            BigDecimal minRatio = ratios.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxRatio = ratios.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (minRatio.compareTo(maxRatio) != 0) {
                highlights.add("报录比差异显著，最低" + minRatio + "，最高" + maxRatio);
            }
        }
        
        return highlights;
    }
}