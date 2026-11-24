package com.xatu.canteen.controller;

import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.service.DishService;
import com.xatu.canteen.service.MenuService;
import com.xatu.canteen.vo.CategoryVO;
import com.xatu.canteen.vo.DishVO;
import com.xatu.canteen.vo.MenuVO;
import com.xatu.canteen.vo.StallVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开的菜品 / 菜单 / 档口查询。
 */
@Tag(name = "03. 菜品与菜单")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final MenuService menuService;

    @Operation(summary = "菜品分类")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(dishService.listCategories());
    }

    @Operation(summary = "菜品分页查询")
    @GetMapping("/dishes")
    public Result<PageResult<DishVO>> dishes(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long stallId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(dishService.pageDishes(categoryId, stallId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "菜品详情（含规格）")
    @GetMapping("/dishes/{id}")
    public Result<DishVO> detail(@PathVariable Long id) {
        return Result.ok(dishService.detail(id));
    }

    @Operation(summary = "今日可点菜单（含实时库存）")
    @GetMapping("/menus/today")
    public Result<List<MenuVO>> today() {
        return Result.ok(menuService.todayList());
    }

    @Operation(summary = "营业中的档口")
    @GetMapping("/stalls")
    public Result<List<StallVO>> stalls() {
        return Result.ok(dishService.listStalls());
    }
}
