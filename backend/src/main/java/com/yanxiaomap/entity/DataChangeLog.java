package com.yanxiaomap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据修改记录表实体类
 * 对应数据库表: data_change_logs
 */
@Data
@TableName("data_change_logs")
public class DataChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 记录唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 记录ID
     */
    private Integer recordId;

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 修改前值
     */
    private String oldValue;

    /**
     * 修改后值
     */
    private String newValue;

    /**
     * 操作管理员ID
     */
    private Integer adminId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 关联的管理员（一对一）
     */
    @TableField(exist = false)
    private Admin admin;
}