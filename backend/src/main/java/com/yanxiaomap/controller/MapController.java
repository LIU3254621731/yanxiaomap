package com.yanxiaomap.controller;

import com.yanxiaomap.common.Result;
import com.yanxiaomap.entity.School;
import com.yanxiaomap.service.SchoolService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图数据控制器
 * 处理地图相关的院校数据查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/map")
@Api(tags = "地图数据接口")
public class MapController {

    @Autowired
    private SchoolService schoolService;

    @Value("${amap.key:}")
    private String amapApiKey;

    @Value("${amap.security-key:}")
    private String amapSecurityKey;

    /**
     * 获取地图院校点位数据
     * 支持按区域、层次、类型等维度筛选
     */
    @GetMapping("/schools")
    @ApiOperation(value = "获取地图院校点位数据", notes = "返回地图上显示的院校点位信息，支持多维度筛选")
    public Result<List<School>> getMapSchools(
            @ApiParam(value = "省份筛选，如'北京市'、'上海市'") @RequestParam(required = false) String province,
            @ApiParam(value = "城市筛选，如'北京市'、'南京市'") @RequestParam(required = false) String city,
            @ApiParam(value = "院校层次筛选，如'985'、'211'、'双一流'、'双非'") @RequestParam(required = false) String level,
            @ApiParam(value = "院校类型筛选，如'综合'、'理工'、'师范'") @RequestParam(required = false) String type,
            @ApiParam(value = "隶属单位筛选，如'教育部'、'省属'") @RequestParam(required = false) String belong,
            @ApiParam(value = "经度范围最小值") @RequestParam(required = false) Double minLng,
            @ApiParam(value = "经度范围最大值") @RequestParam(required = false) Double maxLng,
            @ApiParam(value = "纬度范围最小值") @RequestParam(required = false) Double minLat,
            @ApiParam(value = "纬度范围最大值") @RequestParam(required = false) Double maxLat
    ) {
        log.info("地图院校查询请求: province={}, city={}, level={}, type={}, belong={}, lngRange=[{},{}], latRange=[{},{}]",
                province, city, level, type, belong, minLng, maxLng, minLat, maxLat);

        try {
            // 构建查询参数Map
            Map<String, Object> params = new HashMap<>();
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
            if (minLng != null && maxLng != null) {
                params.put("minLng", minLng);
                params.put("maxLng", maxLng);
            }
            if (minLat != null && maxLat != null) {
                params.put("minLat", minLat);
                params.put("maxLat", maxLat);
            }

            // 调用Service层获取数据
            List<School> schools = schoolService.getSchoolsForMap(params);
            return Result.success("获取院校点位数据成功", schools);
        } catch (Exception e) {
            log.error("获取地图院校数据失败", e);
            return Result.error("获取地图院校数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定院校的详细地图信息
     */
    @GetMapping("/schools/{schoolId}")
    @ApiOperation(value = "获取院校详细地图信息", notes = "根据院校ID获取详细地图信息，包括经纬度、基本信息等")
    public Result<School> getSchoolMapDetail(
            @ApiParam(value = "院校ID", required = true) @PathVariable Integer schoolId
    ) {
        log.info("获取院校地图详情请求: schoolId={}", schoolId);

        try {
            // 调用Service层获取院校信息
            School school = schoolService.getById(schoolId);
            if (school == null) {
                return Result.error("院校不存在");
            }
            return Result.success("获取院校地图详情成功", school);
        } catch (Exception e) {
            log.error("获取院校地图详情失败", e);
            return Result.error("获取院校地图详情失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取院校地图信息
     */
    @PostMapping("/schools/batch")
    @ApiOperation(value = "批量获取院校地图信息", notes = "根据院校ID列表批量获取地图信息")
    public Result<List<School>> getBatchSchoolMapInfo(
            @ApiParam(value = "院校ID列表", required = true) @RequestBody List<Integer> schoolIds
    ) {
        log.info("批量获取院校地图信息请求: schoolIds={}", schoolIds);

        try {
            if (schoolIds == null || schoolIds.isEmpty()) {
                return Result.success("批量获取院校地图信息成功", new ArrayList<>());
            }
            
            // 调用Service层批量获取院校信息
            List<School> schools = schoolService.getSchoolsByIds(schoolIds);
            return Result.success("批量获取院校地图信息成功", schools);
        } catch (Exception e) {
            log.error("批量获取院校地图信息失败", e);
            return Result.error("批量获取院校地图信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取高德地图配置信息
     * 前端通过此接口获取地图API密钥，避免密钥暴露在前端代码中
     */
    @GetMapping("/config")
    @ApiOperation(value = "获取高德地图配置", notes = "返回高德地图API配置信息，包括密钥、安全密钥等")
    public Result<Map<String, String>> getMapConfig() {
        log.info("获取高德地图配置请求");
        
        try {
            Map<String, String> config = new HashMap<>();
            
            // 从配置属性读取密钥（配置属性从环境变量或配置文件读取）
            String apiKey = amapApiKey != null && !amapApiKey.isEmpty() && !amapApiKey.equals("your-amap-api-key") 
                ? amapApiKey : "";
            String securityKey = amapSecurityKey != null && !amapSecurityKey.isEmpty() 
                ? amapSecurityKey : "";
            
            config.put("apiKey", apiKey);
            config.put("securityKey", securityKey);
            config.put("version", "2.0");
            config.put("plugin", "AMap.Geocoder,AMap.AutoComplete,AMap.PlaceSearch");
            config.put("appName", "研校地图-demo");
            
            log.info("高德地图配置读取完成: apiKey存在={}, securityKey存在={}", 
                !apiKey.isEmpty(), !securityKey.isEmpty());
            
            return Result.success("获取高德地图配置成功", config);
        } catch (Exception e) {
            log.error("获取高德地图配置失败", e);
            return Result.error("获取高德地图配置失败: " + e.getMessage());
        }
    }
}