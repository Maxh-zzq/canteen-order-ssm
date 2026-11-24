package com.xatu.canteen.controller;

import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.CartAddDTO;
import com.xatu.canteen.dto.CartUpdateDTO;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.service.CartService;
import com.xatu.canteen.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 购物车接口。
 */
@Tag(name = "04. 购物车")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "查看购物车")
    @GetMapping
    public Result<CartVO> get() {
        return Result.ok(cartService.getCart(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/items")
    public Result<CartVO> add(@Valid @RequestBody CartAddDTO dto) {
        return Result.ok(cartService.add(SecurityUtils.currentUserId(), dto));
    }

    @Operation(summary = "修改数量（0 表示删除）")
    @PutMapping("/items")
    public Result<CartVO> update(@Valid @RequestBody CartUpdateDTO dto) {
        return Result.ok(cartService.update(SecurityUtils.currentUserId(), dto));
    }

    @Operation(summary = "删除单个条目")
    @DeleteMapping("/items")
    public Result<CartVO> remove(@RequestParam Long dishId,
                                 @RequestParam(required = false) Long specId) {
        Long userId = SecurityUtils.currentUserId();
        cartService.remove(userId, dishId, specId);
        return Result.ok(cartService.getCart(userId));
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(SecurityUtils.currentUserId());
        return Result.ok();
    }
}
