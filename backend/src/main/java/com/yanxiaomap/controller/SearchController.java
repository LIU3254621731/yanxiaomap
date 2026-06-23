package com.yanxiaomap.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yanxiaomap.common.Result;
import com.yanxiaomap.entity.School;
import com.yanxiaomap.entity.Major;
import com.yanxiaomap.entity.AdmissionData;
import com.yanxiaomap.service.SchoolService;
import com.yanxiaomap.service.MajorService;
import com.yanxiaomap.service.AdmissionDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 筛选查询控制器
 * 处理院校、专业等复杂筛选查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@Api(tags = "筛选查询接口")
public class SearchController {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private MajorService majorService;

    @Autowired
    private AdmissionDataService admissionDataService;

    /**
     * 院校综合搜索
     * 支持多维度筛选、分页、排序
     */
    @GetMapping("/schools")
    @ApiOperation(value = "院校综合搜索", notes = "支持多维度筛选、分页、排序的院校搜索接口")
    public Result<Page<School>> searchSchools(
            @ApiParam(value = "搜索关键词，院校名称或代码") @RequestParam(required = false) String keyword,
            @ApiParam(value = "省份筛选") @RequestParam(required = false) String province,
            @ApiParam(value = "城市筛选") @RequestParam(required = false) String city,
            @ApiParam(value = "院校层次筛选") @RequestParam(required = false) String level,
            @ApiParam(value = "院校类型筛选") @RequestParam(required = false) String type,
            @ApiParam(value = "隶属单位筛选") @RequestParam(required = false) String belong,
            @ApiParam(value = "页码，从1开始", defaultValue = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小，最大100", defaultValue = "20") @RequestParam(defaultValue = "20") Integer size,
            @ApiParam(value = "排序字段，如'id'、'name'、'level'", defaultValue = "id") @RequestParam(defaultValue = "id") String sortField,
            @ApiParam(value = "排序方向，asc或desc", defaultValue = "desc") @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        log.info("院校搜索请求: keyword={}, province={}, city={}, level={}, type={}, belong={}, page={}, size={}, sortField={}, sortOrder={}",
                keyword, province, city, level, type, belong, page, size, sortField, sortOrder);

        try {
            // 构建查询参数Map
            Map<String, Object> params = new HashMap<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                params.put("keyword", keyword.trim());
            }
            if (province != null && !province.trim().isEmpty()) {
                params.put("province", province.trim());
            }
            if (city != null && !city.trim().isEmpty()) {
                params.put("city", city.trim());
            }
            if (level != null && !level.trim().isEmpty()) {
                params.put("level", level.trim());
            }
            if (type != null && !type.trim().isEmpty()) {
                params.put("type", type.trim());
            }
            if (belong != null && !belong.trim().isEmpty()) {
                params.put("belong", belong.trim());
            }
            params.put("page", page);
            params.put("size", size);
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);

            // 调用Service层进行搜索
            Page<School> schoolPage = schoolService.searchSchools(params);
            return Result.success("搜索成功", schoolPage);
        } catch (Exception e) {
            log.error("院校搜索失败", e);
            return Result.error("院校搜索失败: " + e.getMessage());
        }
    }

    /**
     * 专业搜索
     * 按专业名称、代码、学科门类等搜索
     */
    @GetMapping("/majors")
    @ApiOperation(value = "专业搜索", notes = "按专业名称、代码、学科门类等搜索专业信息")
    public Result<Page<Major>> searchMajors(
            @ApiParam(value = "搜索关键词，专业名称或代码") @RequestParam(required = false) String keyword,
            @ApiParam(value = "学科门类ID筛选") @RequestParam(required = false) Integer categoryId,
            @ApiParam(value = "一级学科ID筛选") @RequestParam(required = false) Integer disciplineId,
            @ApiParam(value = "培养类型筛选，学硕/专硕") @RequestParam(required = false) String type,
            @ApiParam(value = "是否全日制，1=全日制，0=非全日制") @RequestParam(required = false) Integer fullTime,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("专业搜索请求: keyword={}, categoryId={}, disciplineId={}, type={}, fullTime={}, page={}, size={}",
                keyword, categoryId, disciplineId, type, fullTime, page, size);

        try {
            // 构建查询参数Map
            Map<String, Object> params = new HashMap<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                params.put("keyword", keyword.trim());
            }
            if (categoryId != null) {
                params.put("categoryId", categoryId);
            }
            if (disciplineId != null) {
                params.put("disciplineId", disciplineId);
            }
            if (type != null && !type.trim().isEmpty()) {
                params.put("degreeType", type.trim()); // 注意：实体字段名为degreeType
            }
            if (fullTime != null) {
                params.put("fullTime", fullTime);
            }
            params.put("page", page);
            params.put("size", size);

            // 调用Service层进行搜索
            Page<Major> majorPage = majorService.searchMajors(params);
            return Result.success("专业搜索成功", majorPage);
        } catch (Exception e) {
            log.error("专业搜索失败", e);
            return Result.error("专业搜索失败: " + e.getMessage());
        }
    }

    /**
     * 招生数据搜索
     * 按院校、专业、年份等条件搜索招生数据
     */
    @GetMapping("/admission")
    @ApiOperation(value = "招生数据搜索", notes = "按院校、专业、年份等条件搜索招生录取数据")
    public Result<Page<AdmissionData>> searchAdmissionData(
            @ApiParam(value = "院校ID") @RequestParam(required = false) Integer schoolId,
            @ApiParam(value = "专业ID") @RequestParam(required = false) Integer majorId,
            @ApiParam(value = "招生年份") @RequestParam(required = false) Integer year,
            @ApiParam(value = "最低计划招生人数") @RequestParam(required = false) Integer minPlanEnroll,
            @ApiParam(value = "最高计划招生人数") @RequestParam(required = false) Integer maxPlanEnroll,
            @ApiParam(value = "最低报录比") @RequestParam(required = false) Double minAdmissionRatio,
            @ApiParam(value = "最高报录比") @RequestParam(required = false) Double maxAdmissionRatio,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("招生数据搜索请求: schoolId={}, majorId={}, year={}, minPlanEnroll={}, maxPlanEnroll={}, minAdmissionRatio={}, maxAdmissionRatio={}, page={}, size={}",
                schoolId, majorId, year, minPlanEnroll, maxPlanEnroll, minAdmissionRatio, maxAdmissionRatio, page, size);

        try {
            // 构建查询参数Map
            Map<String, Object> params = new HashMap<>();
            if (schoolId != null) {
                params.put("schoolId", schoolId);
            }
            if (majorId != null) {
                params.put("majorId", majorId);
            }
            if (year != null) {
                params.put("year", year);
            }
            if (minPlanEnroll != null) {
                params.put("minPlanEnroll", minPlanEnroll);
            }
            if (maxPlanEnroll != null) {
                params.put("maxPlanEnroll", maxPlanEnroll);
            }
            if (minAdmissionRatio != null) {
                params.put("minAdmissionRatio", minAdmissionRatio);
            }
            if (maxAdmissionRatio != null) {
                params.put("maxAdmissionRatio", maxAdmissionRatio);
            }
            params.put("page", page);
            params.put("size", size);

            // 调用Service层进行搜索
            Page<AdmissionData> admissionPage = admissionDataService.searchAdmissionData(params);
            return Result.success("招生数据搜索成功", admissionPage);
        } catch (Exception e) {
            log.error("招生数据搜索失败", e);
            return Result.error("招生数据搜索失败: " + e.getMessage());
        }
    }

    /**
     * 热门搜索建议
     */
    @GetMapping("/suggestions")
    @ApiOperation(value = "热门搜索建议", notes = "根据输入关键词返回搜索建议，包括院校名、专业名等")
    public Result<List<String>> getSearchSuggestions(
            @ApiParam(value = "输入关键词", required = true) @RequestParam String keyword
    ) {
        log.info("搜索建议请求: keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success("获取搜索建议成功", new ArrayList<>());
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("page", 1);
            params.put("size", 5);

            Page<School> schoolPage = schoolService.searchSchools(params);
            Page<Major> majorPage = majorService.searchMajors(params);

            List<String> suggestions = new ArrayList<>();

            for (School school : schoolPage.getRecords()) {
                suggestions.add("[院校] " + school.getName());
                if (suggestions.size() >= 5) break;
            }

            for (Major major : majorPage.getRecords()) {
                suggestions.add("[专业] " + major.getName());
                if (suggestions.size() >= 10) break;
            }

            return Result.success("获取搜索建议成功", suggestions);
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
            return Result.success("获取搜索建议成功", new ArrayList<>());
        }
    }
}