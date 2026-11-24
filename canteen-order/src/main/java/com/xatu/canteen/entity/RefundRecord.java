package com.xatu.canteen.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水。
 */
@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    /** 退款单号，唯一 */
    private String refundNo;
    private BigDecimal amount;
    /** 1 成功 */
    private Integer status;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
