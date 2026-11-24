package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.OrderStatus;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.ReviewDTO;
import com.xatu.canteen.entity.Dish;
import com.xatu.canteen.entity.OrderInfo;
import com.xatu.canteen.entity.OrderItem;
import com.xatu.canteen.entity.Review;
import com.xatu.canteen.entity.SysUser;
import com.xatu.canteen.mapper.DishMapper;
import com.xatu.canteen.mapper.ReviewMapper;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.security.LoginUser;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评价服务。完成后立即重算相关菜品平均分，保证菜品详情展示及时。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final DishMapper dishMapper;
    private final SysUserMapper userMapper;
    private final OrderService orderService;

    @Transactional(rollbackFor = Exception.class)
    public ReviewVO create(ReviewDTO dto) {
        LoginUser loginUser = SecurityUtils.currentUser();
        OrderInfo order = orderService.requireOrder(dto.getOrderNo());
        if (!Objects.equals(order.getUserId(), loginUser.getId())) {
            throw new BizException(ResultCode.ORDER_OWNER_ERROR);
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.COMPLETED.getCode()) {
            throw new BizException(ResultCode.REVIEW_NOT_ALLOWED);
        }
        long exists = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId()));
        if (exists > 0) {
            throw new BizException(ResultCode.REVIEW_EXISTS);
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setOrderNo(order.getOrderNo());
        review.setUserId(loginUser.getId());
        review.setUsername(displayName(loginUser.getId()));
        review.setStallId(order.getStallId());
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages());
        reviewMapper.insert(review);

        List<OrderItem> items = orderService.listItems(order.getId());
        List<Long> dishIds = items.stream().map(OrderItem::getDishId).distinct().collect(Collectors.toList());
        for (Long dishId : dishIds) {
            BigDecimal avg = reviewMapper.avgScoreOfDish(dishId);
            dishMapper.updateAvgScore(dishId, avg == null ? BigDecimal.ZERO
                    : avg.setScale(1, RoundingMode.HALF_UP));
        }
        return ReviewVO.from(review);
    }

    public List<ReviewVO> listByDish(Long dishId, Integer limit) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 50);
        List<Review> reviews = reviewMapper.listByDish(dishId, size);
        return reviews.stream().map(ReviewVO::from).collect(Collectors.toList());
    }

    private String displayName(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user != null && StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (user != null && StringUtils.hasText(user.getUsername())) {
            String username = user.getUsername();
            return username.length() <= 2 ? username
                    : username.substring(0, 2) + "****" + username.substring(username.length() - 2);
        }
        return "匿名同学";
    }
}
