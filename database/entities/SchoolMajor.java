package com.yanxiaomap.database.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 院校-专业关联表实体类
 * 对应数据库表: school_majors
 */
@Data
@TableName("school_majors")
public class SchoolMajor implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 关联唯一ID
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
     * 是否招生，1=招生，0=暂停招生
     */
    private Integer status;

    /**
     * 关联备注，如招生院系、特殊要求
     */
    private String remark;

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
    @TableLogic
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