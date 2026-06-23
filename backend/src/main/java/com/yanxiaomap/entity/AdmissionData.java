package com.yanxiaomap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 招生录取数据表实体类
 * 对应数据库表: admission_data
 */
@Data
@TableName("admission_data")
public class AdmissionData implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 数据唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 院校ID，外键关联schools表id
     */
    private Integer schoolId;

    /**
     * 专业ID，外键关联majors表id
     */
    private Integer majorId;

    /**
     * 招生年份，2017-2024
     */
    private Integer year;

    /**
     * 计划招生人数，非负整数
     */
    private Integer planEnroll;

    /**
     * 实际录取人数（含推免），非负整数
     */
    private Integer actualEnroll;

    /**
     * 推免人数，非负整数
     */
    private Integer recommendedCount;

    /**
     * 报录比，保留2位小数
     */
    private BigDecimal admissionRatio;

    /**
     * 复试总分线
     */
    private Integer retestTotalScore;

    /**
     * 单科线
     */
    private Integer singleSubjectScore;

    /**
     * 录取平均分
     */
    private Integer averageAdmissionScore;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 关联的院校（一对一）
     */
    @TableField(exist = false)
    private School school;

    /**
     * 关联的专业（一对一）
     */
    @TableField(exist = false)
    private Major major;
}