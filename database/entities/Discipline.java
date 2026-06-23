package com.yanxiaomap.database.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一级学科表实体类
 * 对应数据库表: disciplines
 */
@Data
@TableName("disciplines")
public class Discipline implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 一级学科唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 所属学科门类ID，外键关联subject_categories表id
     */
    private Integer categoryId;

    /**
     * 一级学科官方代码
     */
    private String code;

    /**
     * 一级学科全称
     */
    private String name;

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
     * 关联的学科门类（一对一）
     */
    @TableField(exist = false)
    private SubjectCategory category;
}