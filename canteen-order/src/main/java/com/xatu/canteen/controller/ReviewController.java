package com.xatu.canteen.controller;

import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.ReviewDTO;
import com.xatu.canteen.service.ReviewService;
import com.xatu.canteen.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 评价接口。
 */
@Tag(name = "08. 评价")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交订单评价（仅已完成订单，一次）")
    @PostMapping("/reviews")
    public Result<ReviewVO> create(@Valid @RequestBody ReviewDTO dto) {
        return Result.ok(reviewService.create(dto));
    }

    @Operation(summary = "菜品最新评价")
    @GetMapping("/dishes/{dishId}/reviews")
    public Result<List<ReviewVO>> listByDish(@PathVariable Long dishId,
                                             @RequestParam(defaultValue = "20") Integer limit) {
        return Result.ok(reviewService.listByDish(dishId, limit));
    }
}
