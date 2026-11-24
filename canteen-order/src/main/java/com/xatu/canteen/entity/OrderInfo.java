package com.xatu.canteen.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表。
 */
@Data
@TableName("order_info")
public class OrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long stallId;
    /** 见 OrderStatus */
    private Integer status;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String pickupCode;
    private LocalDateTime expectTime;
    /** 支付截止时间 */
    private LocalDateTime expireTime;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private String cancelReason;
    private String remark;
    /** 客户端幂等号 */
    private String requestId;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
