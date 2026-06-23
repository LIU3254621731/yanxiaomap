package com.yanxiaomap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专业表实体类
 * 对应数据库表: majors
 */
@Data
@TableName("majors")
public class Major implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 专业唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 所属一级学科ID，外键关联disciplines表id
     */
    private Integer disciplineId;

    /**
     * 学科门类名称（冗余字段，便于前端展示）
     */
    @TableField(exist = false)
    private String category;

    /**
     * 专业官方6位代码，唯一
     */
    private String code;

    /**
     * 专业全称，官方完整名称
     */
    private String name;

    /**
     * 专业别名，多个别名用逗号分隔，用于搜索
     */
    private String alias;

    /**
     * 培养类型，枚举：学硕/专硕
     */
    private String type;

    /**
     * 学位类型：学术学位/专业学位
     */
    private String degreeType;

    /**
     * 学制年数
     */
    private Integer duration;

    /**
     * 学科门类ID
     */
    private Integer categoryId;

    /**
     * 是否全日制，1=全日制，0=非全日制
     */
    private Integer fullTime;

    /**
     * 专业简介/描述
     */
    private String description;

    /**
     * 培养目标
     */
    private String trainingObjective;

    /**
     * 主要课程
     */
    private String mainCourses;

    /**
     * 就业方向
     */
    private String employmentDirection;

    /**
     * 专业状态，1=启用，0=禁用
     */
    private Integer status;

    /**
     * 专业备注，特殊培养方向等
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
    private LocalDateTime deletedAt;

    /**
     * 关联的一级学科（一对一）
     */
    @TableField(exist = false)
    private Discipline discipline;
}