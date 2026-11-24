package com.xatu.canteen.controller;

import com.xatu.canteen.common.OperLog;
import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.CategoryDTO;
import com.xatu.canteen.dto.DishDTO;
import com.xatu.canteen.dto.MenuPublishDTO;
import com.xatu.canteen.dto.MenuStockDTO;
import com.xatu.canteen.dto.StallDTO;
import com.xatu.canteen.entity.Stall;
import com.xatu.canteen.service.DishService;
import com.xatu.canteen.service.FileService;
import com.xatu.canteen.service.MenuService;
import com.xatu.canteen.vo.CategoryVO;
import com.xatu.canteen.vo.DishVO;
import com.xatu.canteen.vo.MenuVO;
import com.xatu.canteen.vo.StallVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理端：分类、菜品、每日菜单、档口、图片上传。
 */
@Tag(name = "09. 管理端-菜品")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDishController {

    private final DishService dishService;
    private final MenuService menuService;
    private final FileService fileService;

    // ---------- 分类 ----------

    @Operation(summary = "管理端分类列表（含停用）")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(dishService.listAllCategories());
    }

    @Operation(summary = "新增分类")
    @OperLog(module = "菜品管理", action = "新增分类")
    @PostMapping("/categories")
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryDTO dto) {
        return Result.ok(dishService.createCategory(dto));
    }

    @Operation(summary = "修改分类")
    @OperLog(module = "菜品管理", action = "修改分类")
    @PutMapping("/categories/{id}")
    public Result<CategoryVO> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return Result.ok(dishService.updateCategory(id, dto));
    }

    @Operation(summary = "删除分类")
    @OperLog(module = "菜品管理", action = "删除分类")
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        dishService.deleteCategory(id);
        return Result.ok();
    }

    // ---------- 菜品 ----------

    @Operation(summary = "管理端菜品分页（含停用）")
    @GetMapping("/dishes")
    public Result<PageResult<DishVO>> dishes(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long stallId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.ok(dishService.pageAllDishes(categoryId, stallId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "管理端菜品详情（含停用）")
    @GetMapping("/dishes/{id}")
    public Result<DishVO> dishDetail(@PathVariable Long id) {
        return Result.ok(dishService.adminDetail(id));
    }

    @Operation(summary = "新增菜品")
    @OperLog(module = "菜品管理", action = "新增菜品")
    @PostMapping("/dishes")
    public Result<DishVO> createDish(@Valid @RequestBody DishDTO dto) {
        return Result.ok(dishService.createDish(dto));
    }

    @Operation(summary = "修改菜品")
    @OperLog(module = "菜品管理", action = "修改菜品")
    @PutMapping("/dishes/{id}")
    public Result<DishVO> updateDish(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        return Result.ok(dishService.updateDish(id, dto));
    }

    @Operation(summary = "删除菜品")
    @OperLog(module = "菜品管理", action = "删除菜品")
    @DeleteMapping("/dishes/{id}")
    public Result<Void> deleteDish(@PathVariable Long id) {
        dishService.deleteDish(id);
        return Result.ok();
    }

    // ---------- 每日菜单 ----------

    @Operation(summary = "发布每日菜单")
    @OperLog(module = "菜单管理", action = "发布菜单")
    @PostMapping("/menus/publish")
    public Result<Integer> publish(@RequestBody MenuPublishDTO dto) {
        return Result.ok(menuService.publish(dto));
    }

    @Operation(summary = "查看某日菜单")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(menuService.listByDate(date));
    }

    @Operation(summary = "调整菜单总供应量")
    @OperLog(module = "菜单管理", action = "调整库存")
    @PutMapping("/menus/{id}/stock")
    public Result<MenuVO> updateStock(@PathVariable Long id, @Valid @RequestBody MenuStockDTO dto) {
        return Result.ok(menuService.updateStock(id, dto.getStockTotal()));
    }

    // ---------- 档口 ----------

    @Operation(summary = "管理端档口列表（含停用）")
    @GetMapping("/stalls")
    public Result<List<StallVO>> stalls() {
        return Result.ok(dishService.listAllStalls());
    }

    @Operation(summary = "新增档口")
    @OperLog(module = "档口管理", action = "新增档口")
    @PostMapping("/stalls")
    public Result<StallVO> createStall(@Valid @RequestBody StallDTO dto) {
        return Result.ok(dishService.createStall(toEntity(dto)));
    }

    @Operation(summary = "修改档口")
    @OperLog(module = "档口管理", action = "修改档口")
    @PutMapping("/stalls/{id}")
    public Result<StallVO> updateStall(@PathVariable Long id, @Valid @RequestBody StallDTO dto) {
        return Result.ok(dishService.updateStall(id, toEntity(dto)));
    }

    @Operation(summary = "删除档口")
    @OperLog(module = "档口管理", action = "删除档口")
    @DeleteMapping("/stalls/{id}")
    public Result<Void> deleteStall(@PathVariable Long id) {
        dishService.deleteStall(id);
        return Result.ok();
    }

    // ---------- 文件 ----------

    @Operation(summary = "上传菜品图片")
    @OperLog(module = "文件管理", action = "上传图片")
    @PostMapping("/files/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    private Stall toEntity(StallDTO dto) {
        Stall stall = new Stall();
        stall.setName(dto.getName());
        stall.setFloor(dto.getFloor());
        stall.setWindowNo(dto.getWindowNo());
        stall.setManagerId(dto.getManagerId());
        stall.setOpenTime(dto.getOpenTime());
        stall.setCloseTime(dto.getCloseTime());
        stall.setStatus(dto.getStatus());
        return stall;
    }
}
