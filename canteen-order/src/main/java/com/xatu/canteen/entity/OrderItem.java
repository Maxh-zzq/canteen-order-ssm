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
 * 订单明细。下单时写入菜品名称与价格快照，后续改价不影响历史订单。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long dishId;
    /** 下单时对应的当日菜单，用于取消/退款回滚库存 */
    private Long menuId;
    private String dishName;
    private String specName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
