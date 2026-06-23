package com.yanxiaomap.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yanxiaomap.entity.AdmissionData;
import java.util.List;
import java.util.Map;

/**
 * 招生录取数据服务接口
 */
public interface AdmissionDataService extends IService<AdmissionData> {

    /**
     * 综合搜索招生数据
     */
    Page<AdmissionData> searchAdmissionData(Map<String, Object> params);

    /**
     * 根据院校ID和专业ID查询招生数据
     */
    List<AdmissionData> getBySchoolIdAndMajorId(Integer schoolId, Integer majorId);

    /**
     * 根据院校ID查询招生数据
     */
    List<AdmissionData> getBySchoolId(Integer schoolId);

    /**
     * 根据专业ID查询招生数据
     */
    List<AdmissionData> getByMajorId(Integer majorId);

    /**
     * 查询指定年份的招生数据
     */
    List<AdmissionData> getByYear(Integer year);

    /**
     * 获取院校历年招生数据统计
     */
    Map<String, Object> getSchoolAdmissionHistory(Integer schoolId, Integer startYear, Integer endYear);

    /**
     * 获取专业历年招生数据统计
     */
    Map<String, Object> getMajorAdmissionHistory(Integer majorId, Integer startYear, Integer endYear);

    /**
     * 批量获取招生数据
     */
    List<AdmissionData> getAdmissionDataByIds(List<Integer> admissionIds);

    /**
     * 获取招生数据统计信息
     */
    Map<String, Long> getAdmissionStatistics();

    /**
     * 检查重复数据
     */
    boolean checkDuplicate(Integer schoolId, Integer majorId, Integer year);
}