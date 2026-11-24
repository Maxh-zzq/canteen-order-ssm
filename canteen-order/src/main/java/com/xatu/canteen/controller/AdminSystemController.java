package com.xatu.canteen.controller;

import com.xatu.canteen.common.OperLog;
import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.ConfigDTO;
import com.xatu.canteen.dto.UserStallDTO;
import com.xatu.canteen.dto.UserStatusDTO;
import com.xatu.canteen.service.AdminService;
import com.xatu.canteen.service.StatsService;
import com.xatu.canteen.vo.ConfigVO;
import com.xatu.canteen.vo.OperLogVO;
import com.xatu.canteen.vo.SalesTrendVO;
import com.xatu.canteen.vo.StatsOverviewVO;
import com.xatu.canteen.vo.TopDishVO;
import com.xatu.canteen.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理端：用户、日志、参数、统计报表。
 */
@Tag(name = "10. 管理端-系统")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final AdminService adminService;
    private final StatsService statsService;

    @Operation(summary = "用户分页（账号/姓名模糊）")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminService.pageUsers(keyword, pageNum, pageSize));
    }

    @Operation(summary = "启用/禁用用户")
    @OperLog(module = "用户管理", action = "修改用户状态")
    @PutMapping("/users/{id}/status")
    public Result<UserVO> updateUserStatus(@PathVariable Long id,
                                           @Valid @RequestBody UserStatusDTO dto) {
        return Result.ok(adminService.updateUserStatus(id, dto.getStatus()));
    }

    @Operation(summary = "查看员工档口权限")
    @GetMapping("/users/{id}/stalls")
    public Result<List<Long>> userStalls(@PathVariable Long id) {
        return Result.ok(adminService.listUserStalls(id));
    }

    @Operation(summary = "分配员工档口权限（整体替换）")
    @OperLog(module = "用户管理", action = "分配档口权限")
    @PutMapping("/users/{id}/stalls")
    public Result<List<Long>> updateUserStalls(@PathVariable Long id,
                                               @Valid @RequestBody UserStallDTO dto) {
        return Result.ok(adminService.updateUserStalls(id, dto.getStallIds()));
    }

    @Operation(summary = "操作日志分页")
    @GetMapping("/logs")
    public Result<PageResult<OperLogVO>> logs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminService.pageLogs(pageNum, pageSize));
    }

    @Operation(summary = "参数配置列表")
    @GetMapping("/configs")
    public Result<List<ConfigVO>> configs() {
        return Result.ok(adminService.listConfigs());
    }

    @Operation(summary = "新增/修改参数配置")
    @OperLog(module = "系统配置", action = "保存参数")
    @PutMapping("/configs")
    public Result<ConfigVO> saveConfig(@Valid @RequestBody ConfigDTO dto) {
        return Result.ok(adminService.saveConfig(dto));
    }

    @Operation(summary = "经营概览")
    @GetMapping("/stats/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(statsService.overview());
    }

    @Operation(summary = "销售趋势（默认 7 天）")
    @GetMapping("/stats/sales-trend")
    public Result<List<SalesTrendVO>> salesTrend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.ok(statsService.salesTrend(days));
    }

    @Operation(summary = "热销菜品")
    @GetMapping("/stats/top-dishes")
    public Result<List<TopDishVO>> topDishes(@RequestParam(defaultValue = "7") Integer days,
                                             @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(statsService.topDishes(days, limit));
    }

    @Operation(summary = "导出订单 CSV")
    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] data = statsService.exportOrders(startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\"");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
