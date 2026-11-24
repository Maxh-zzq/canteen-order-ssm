package com.xatu.canteen.controller;

import com.xatu.canteen.common.Result;
import com.xatu.canteen.service.StaffStallService;
import com.xatu.canteen.vo.StallVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工数据范围接口：只返回当前员工有权限的档口。
 */
@Tag(name = "06. 档口出餐")
@RestController
@RequestMapping("/api/v1/staff/stalls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffStallController {

    private final StaffStallService staffStallService;

    @Operation(summary = "我的档口（STAFF 返回绑定档口，ADMIN 返回全部）")
    @GetMapping
    public Result<List<StallVO>> myStalls() {
        return Result.ok(staffStallService.myStalls());
    }
}
