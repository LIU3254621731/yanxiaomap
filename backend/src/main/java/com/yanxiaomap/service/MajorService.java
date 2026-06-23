package com.yanxiaomap.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yanxiaomap.entity.Major;
import java.util.List;
import java.util.Map;

/**
 * 专业服务接口
 */
public interface MajorService extends IService<Major> {

    /**
     * 综合搜索专业
     */
    Page<Major> searchMajors(Map<String, Object> params);

    /**
     * 根据ID获取专业详情（包含关联信息）
     */
    Map<String, Object> getMajorDetail(Integer majorId);

    /**
     * 根据一级学科ID获取专业列表
     */
    List<Major> getMajorsByDisciplineId(Integer disciplineId);

    /**
     * 根据学科门类ID获取专业列表
     */
    List<Major> getMajorsByCategoryId(Integer categoryId);

    /**
     * 批量获取专业信息
     */
    List<Major> getMajorsByIds(List<Integer> majorIds);

    /**
     * 检查专业代码是否已存在
     */
    boolean checkCodeExists(String code, Integer excludeId);

    /**
     * 获取专业数量统计
     */
    Map<String, Long> getMajorStatistics();

    /**
     * 根据关键词搜索专业（包含别名搜索）
     */
    List<Major> searchMajorsByKeyword(String keyword);
}