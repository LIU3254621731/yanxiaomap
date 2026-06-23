package com.yanxiaomap.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yanxiaomap.common.Result;
import com.yanxiaomap.entity.*;
import com.yanxiaomap.mapper.DisciplineMapper;
import com.yanxiaomap.mapper.SchoolMajorMapper;
import com.yanxiaomap.mapper.SubjectCategoryMapper;
import com.yanxiaomap.service.SchoolService;
import com.yanxiaomap.service.MajorService;
import com.yanxiaomap.service.AdmissionDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 详情信息控制器
 * 处理院校、专业、招生数据等详情信息查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/details")
@Api(tags = "详情信息接口")
public class DetailController {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private MajorService majorService;

    @Autowired
    private AdmissionDataService admissionDataService;

    @Autowired
    private SchoolMajorMapper schoolMajorMapper;

    @Autowired
    private DisciplineMapper disciplineMapper;

    @Autowired
    private SubjectCategoryMapper subjectCategoryMapper;

    /**
     * 获取院校详情信息
     */
    @GetMapping("/schools/{schoolId}")
    @ApiOperation(value = "获取院校详情信息", notes = "根据院校ID获取完整的院校详情信息，包括开设专业和招生数据")
    public Result<Map<String, Object>> getSchoolDetail(
            @ApiParam(value = "院校ID", required = true) @PathVariable Integer schoolId
    ) {
        log.info("获取院校详情请求: schoolId={}", schoolId);

        try {
            School school = schoolService.getById(schoolId);
            if (school == null) {
                return Result.error("院校不存在");
            }

            LambdaQueryWrapper<SchoolMajor> smWrapper = Wrappers.lambdaQuery();
            smWrapper.eq(SchoolMajor::getSchoolId, schoolId);
            List<SchoolMajor> schoolMajors = schoolMajorMapper.selectList(smWrapper);
            List<Integer> majorIds = schoolMajors.stream()
                    .map(SchoolMajor::getMajorId)
                    .collect(Collectors.toList());

            List<Major> majors = majorIds.isEmpty() ? new ArrayList<>() : majorService.getMajorsByIds(majorIds);
            List<AdmissionData> admissionDataList = admissionDataService.getBySchoolId(schoolId);

            Map<String, Object> result = new HashMap<>();
            result.put("school", school);
            result.put("majors", majors);
            result.put("admissionData", admissionDataList);

            Map<String, Object> stats = new HashMap<>();
            stats.put("majorCount", majors.size());
            stats.put("admissionDataCount", admissionDataList.size());
            result.put("statistics", stats);

            return Result.success("获取院校详情成功", result);
        } catch (Exception e) {
            log.error("获取院校详情失败", e);
            return Result.error("获取院校详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取专业详情信息
     */
    @GetMapping("/majors/{majorId}")
    @ApiOperation(value = "获取专业详情信息", notes = "根据专业ID获取完整的专业详情信息，包括所属学科门类、开设院校和招生数据")
    public Result<Map<String, Object>> getMajorDetail(
            @ApiParam(value = "专业ID", required = true) @PathVariable Integer majorId
    ) {
        log.info("获取专业详情请求: majorId={}", majorId);

        try {
            Major major = majorService.getById(majorId);
            if (major == null) {
                return Result.error("专业不存在");
            }

            Discipline discipline = major.getDisciplineId() != null ?
                    disciplineMapper.selectById(major.getDisciplineId()) : null;
            SubjectCategory category = major.getCategoryId() != null ?
                    subjectCategoryMapper.selectById(major.getCategoryId()) : null;

            LambdaQueryWrapper<SchoolMajor> smWrapper = Wrappers.lambdaQuery();
            smWrapper.eq(SchoolMajor::getMajorId, majorId);
            List<SchoolMajor> schoolMajors = schoolMajorMapper.selectList(smWrapper);
            List<Integer> schoolIds = schoolMajors.stream()
                    .map(SchoolMajor::getSchoolId)
                    .collect(Collectors.toList());

            List<School> schools = schoolIds.isEmpty() ? new ArrayList<>() : schoolService.getSchoolsByIds(schoolIds);
            List<AdmissionData> admissionDataList = admissionDataService.getByMajorId(majorId);

            Map<String, Object> result = new HashMap<>();
            result.put("major", major);
            result.put("discipline", discipline);
            result.put("category", category);
            result.put("schools", schools);
            result.put("admissionData", admissionDataList);

            Map<String, Object> stats = new HashMap<>();
            stats.put("schoolCount", schools.size());
            stats.put("admissionDataCount", admissionDataList.size());
            result.put("statistics", stats);

            return Result.success("获取专业详情成功", result);
        } catch (Exception e) {
            log.error("获取专业详情失败", e);
            return Result.error("获取专业详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取院校-专业详情信息
     * 针对特定院校的特定专业
     */
    @GetMapping("/schools/{schoolId}/majors/{majorId}")
    @ApiOperation(value = "获取院校-专业详情信息", notes = "根据院校ID和专业ID获取特定院校的特定专业详情信息")
    public Result<Map<String, Object>> getSchoolMajorDetail(
            @ApiParam(value = "院校ID", required = true) @PathVariable Integer schoolId,
            @ApiParam(value = "专业ID", required = true) @PathVariable Integer majorId
    ) {
        log.info("获取院校-专业详情请求: schoolId={}, majorId={}", schoolId, majorId);

        try {
            // 查询院校信息
            School school = schoolService.getById(schoolId);
            if (school == null) {
                return Result.error("院校不存在");
            }
            
            // 查询专业信息
            Major major = majorService.getById(majorId);
            if (major == null) {
                return Result.error("专业不存在");
            }
            
            // 查询招生历史数据
            List<AdmissionData> admissionHistory = admissionDataService.getBySchoolIdAndMajorId(schoolId, majorId);
            
            // 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("school", school);
            result.put("major", major);
            result.put("admissionHistory", admissionHistory);
            
            // 计算统计信息
            if (!admissionHistory.isEmpty()) {
                Map<String, Object> stats = new HashMap<>();
                double avgScore = admissionHistory.stream()
                    .mapToInt(AdmissionData::getAverageAdmissionScore)
                    .average()
                    .orElse(0.0);
                int totalEnrollment = admissionHistory.stream()
                    .mapToInt(AdmissionData::getActualEnroll)
                    .sum();
                int minYear = admissionHistory.stream()
                    .mapToInt(AdmissionData::getYear)
                    .min()
                    .orElse(0);
                int maxYear = admissionHistory.stream()
                    .mapToInt(AdmissionData::getYear)
                    .max()
                    .orElse(0);
                
                stats.put("averageScore", avgScore);
                stats.put("totalEnrollment", totalEnrollment);
                stats.put("yearRange", minYear + "-" + maxYear);
                stats.put("dataCount", admissionHistory.size());
                result.put("statistics", stats);
            }
            
            return Result.success("获取院校-专业详情成功", result);
        } catch (Exception e) {
            log.error("获取院校-专业详情失败", e);
            return Result.error("获取院校-专业详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取招生数据详情
     */
    @GetMapping("/admission/{admissionId}")
    @ApiOperation(value = "获取招生数据详情", notes = "根据招生数据ID获取完整的招生数据详情，包括关联院校和专业信息")
    public Result<Map<String, Object>> getAdmissionDetail(
            @ApiParam(value = "招生数据ID", required = true) @PathVariable Integer admissionId
    ) {
        log.info("获取招生数据详情请求: admissionId={}", admissionId);

        try {
            AdmissionData admissionData = admissionDataService.getById(admissionId);
            if (admissionData == null) {
                return Result.error("招生数据不存在");
            }

            School school = schoolService.getById(admissionData.getSchoolId());
            Major major = majorService.getById(admissionData.getMajorId());

            Map<String, Object> result = new HashMap<>();
            result.put("admissionData", admissionData);
            result.put("school", school);
            result.put("major", major);

            return Result.success("获取招生数据详情成功", result);
        } catch (Exception e) {
            log.error("获取招生数据详情失败", e);
            return Result.error("获取招生数据详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取院校历年招生数据
     */
    @GetMapping("/schools/{schoolId}/admission/history")
    @ApiOperation(value = "获取院校历年招生数据", notes = "获取院校历年的招生数据统计，按年份分组")
    public Result<Map<String, Object>> getSchoolAdmissionHistory(
            @ApiParam(value = "院校ID", required = true) @PathVariable Integer schoolId,
            @ApiParam(value = "起始年份", defaultValue = "2017") @RequestParam(defaultValue = "2017") Integer startYear,
            @ApiParam(value = "结束年份", defaultValue = "2024") @RequestParam(defaultValue = "2024") Integer endYear
    ) {
        log.info("获取院校历年招生数据请求: schoolId={}, startYear={}, endYear={}", schoolId, startYear, endYear);

        try {
            School school = schoolService.getById(schoolId);
            if (school == null) {
                return Result.error("院校不存在");
            }

            Map<String, Object> historyData = admissionDataService.getSchoolAdmissionHistory(schoolId, startYear, endYear);

            Map<String, Object> result = new HashMap<>();
            result.put("school", school);
            result.put("admissionData", historyData.get("admissionData"));
            result.put("summary", historyData);

            return Result.success("获取院校历年招生数据成功", result);
        } catch (Exception e) {
            log.error("获取院校历年招生数据失败", e);
            return Result.error("获取院校历年招生数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取专业历年招生数据
     */
    @GetMapping("/majors/{majorId}/admission/history")
    @ApiOperation(value = "获取专业历年招生数据", notes = "获取专业历年的招生数据统计，按年份分组")
    public Result<Map<String, Object>> getMajorAdmissionHistory(
            @ApiParam(value = "专业ID", required = true) @PathVariable Integer majorId,
            @ApiParam(value = "起始年份", defaultValue = "2017") @RequestParam(defaultValue = "2017") Integer startYear,
            @ApiParam(value = "结束年份", defaultValue = "2024") @RequestParam(defaultValue = "2024") Integer endYear
    ) {
        log.info("获取专业历年招生数据请求: majorId={}, startYear={}, endYear={}", majorId, startYear, endYear);

        try {
            Major major = majorService.getById(majorId);
            if (major == null) {
                return Result.error("专业不存在");
            }

            Map<String, Object> historyData = admissionDataService.getMajorAdmissionHistory(majorId, startYear, endYear);

            Map<String, Object> result = new HashMap<>();
            result.put("major", major);
            result.put("admissionData", historyData.get("admissionData"));
            result.put("summary", historyData);

            return Result.success("获取专业历年招生数据成功", result);
        } catch (Exception e) {
            log.error("获取专业历年招生数据失败", e);
            return Result.error("获取专业历年招生数据失败: " + e.getMessage());
        }
    }
}