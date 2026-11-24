package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页入参基类。
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private Integer pageSize = 10;
}
