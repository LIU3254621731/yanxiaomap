package com.yanxiaomap.database.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员表实体类
 * 对应数据库表: admins
 */
@Data
@TableName("admins")
public class Admin implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 管理员唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 管理员账号
     */
    private String username;

    /**
     * 加密密码，使用BCrypt加密
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态，1=启用，0=禁用
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 锁定直到时间
     */
    private LocalDateTime lockedUntil;

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