package com.yanxiaomap.database.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学科门类表实体类
 * 对应数据库表: subject_categories
 */
@Data
@TableName("subject_categories")
public class SubjectCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 学科门类唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 门类官方代码（如01哲学）
     */
    private String code;

    /**
     * 学科门类全称
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
}