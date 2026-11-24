package com.xatu.canteen.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 发布每日菜单。serveDate 为空则发布今天；dishIds 为空则发布全部上架菜品。
 */
@Data
public class MenuPublishDTO {

    private LocalDate serveDate;
    private List<Long> dishIds;
}
