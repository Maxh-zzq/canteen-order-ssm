package com.xatu.canteen.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工与档口的绑定关系，是 STAFF 数据权限的唯一来源。
 * 一个员工可以绑定多个档口；status + 生效时间控制当前是否可用。
 */
@Data
@TableName("staff_stall")
public class StaffStall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long stallId;
    /** 1 有效，0 停用 */
    private Integer status;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
