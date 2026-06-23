package com.yanxiaomap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 院校表实体类
 * 对应数据库表: schools
 */
@Data
@TableName("schools")
public class School implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 院校唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 院校全称，官方完整名称
     */
    private String name;

    /**
     * 教育部官方5位院校代码，唯一
     */
    private String code;

    /**
     * 所在省份，省级行政区全称
     */
    private String province;

    /**
     * 所在城市，市级行政区全称
     */
    private String city;

    /**
     * 院校层次，枚举：985/211/双一流/双非
     */
    private String level;

    /**
     * 院校类型，枚举：综合/理工/师范/医药/财经等
     */
    private String type;

    /**
     * 隶属单位，枚举：教育部/省属/市属/其他
     */
    private String belong;

    /**
     * 经度，高德地图查询，精确到6位小数
     */
    private java.math.BigDecimal longitude;

    /**
     * 纬度，高德地图查询，精确到6位小数
     */
    private java.math.BigDecimal latitude;

    /**
     * 院校官网，完整网址
     */
    private String website;

    /**
     * 招生单位，如研究生院、各院系
     */
    private String enrollmentUnit;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 院校简介
     */
    private String introduction;

    /**
     * 建校年份
     */
    private Integer establishedYear;

    /**
     * 院校LOGO地址，后期扩展
     */
    private String logo;

    /**
     * 院校状态，1=启用（地图显示），0=禁用（不显示）
     */
    private Integer status;

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
}