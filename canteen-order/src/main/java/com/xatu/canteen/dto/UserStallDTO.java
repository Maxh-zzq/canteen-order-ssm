package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 管理员为员工分配档口权限。空数组表示取消全部档口权限。
 */
@Data
public class UserStallDTO {

    @NotNull(message = "档口列表不能为空")
    private List<Long> stallIds;
}
