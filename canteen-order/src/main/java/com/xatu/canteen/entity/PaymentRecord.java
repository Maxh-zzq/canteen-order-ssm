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
 * 支付流水。trade_no 唯一索引保证回调幂等。
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private String tradeNo;
    /** BALANCE / MOCK */
    private String channel;
    private BigDecimal amount;
    /** 1 成功 */
    private Integer status;
    private LocalDateTime callbackTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
