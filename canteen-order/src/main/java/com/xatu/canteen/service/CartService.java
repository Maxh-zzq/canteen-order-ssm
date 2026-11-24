package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.CartAddDTO;
import com.xatu.canteen.dto.CartUpdateDTO;
import com.xatu.canteen.entity.CartItem;
import com.xatu.canteen.entity.DailyMenu;
import com.xatu.canteen.entity.Dish;
import com.xatu.canteen.entity.DishSpec;
import com.xatu.canteen.mapper.CartItemMapper;
import com.xatu.canteen.vo.CartItemVO;
import com.xatu.canteen.vo.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 购物车：Redis Hash 为主（field = dishId:specId），数据库 cart_item 兜底并保持同步。
 * Redis 故障时自动走数据库，功能不中断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Duration CART_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final CartItemMapper cartItemMapper;
    private final DishService dishService;
    private final MenuService menuService;
    private final StockService stockService;

    public CartVO getCart(Long userId) {
        List<CartItem> lines = loadRawItems(userId);
        CartVO vo = new CartVO();
        if (lines.isEmpty()) {
            return vo;
        }
        Map<Long, Dish> dishes = loadDishes(lines);
        Map<Long, DishSpec> specs = loadSpecs(lines);
        Map<Long, DailyMenu> menus = menuService.findTodayMenus(
                lines.stream().map(CartItem::getDishId).distinct().collect(java.util.stream.Collectors.toList()));

        BigDecimal total = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<CartItemVO> items = new ArrayList<>();
        for (CartItem line : lines) {
            Dish dish = dishes.get(line.getDishId());
            if (dish == null) {
                continue;
            }
            DishSpec spec = line.getSpecId() == null || line.getSpecId() == 0L ? null : specs.get(line.getSpecId());
            BigDecimal unitPrice = unitPrice(dish, spec);
            int quantity = line.getQuantity() == null ? 0 : line.getQuantity();
            if (quantity <= 0) {
                continue;
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            CartItemVO item = new CartItemVO();
            item.setDishId(dish.getId());
            item.setSpecId(line.getSpecId() == null ? 0L : line.getSpecId());
            item.setSpecName(spec == null ? null : spec.getName());
            item.setDishName(dish.getName());
            item.setCoverUrl(dish.getCoverUrl());
            item.setPrice(unitPrice);
            item.setQuantity(quantity);
            item.setAmount(amount);
            DailyMenu menu = menus.get(dish.getId());
            if (menu != null) {
                item.setStockLeft(stockService.currentStock(menu.getId(),
                        menu.getStockLeft() == null ? 0 : menu.getStockLeft()));
            } else {
                item.setStockLeft(0);
            }
            items.add(item);
            total = total.add(amount);
            totalQuantity += quantity;
        }
        vo.setItems(items);
        vo.setTotalQuantity(totalQuantity);
        vo.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public CartVO add(Long userId, CartAddDTO dto) {
        Dish dish = dishService.requireDish(dto.getDishId());
        DishSpec spec = dishService.requireSpec(dish.getId(), dto.getSpecId());
        DailyMenu menu = menuService.findTodayMenu(dish.getId());
        if (menu == null) {
            throw new BizException(ResultCode.MENU_NOT_FOUND);
        }
        Long specId = dto.getSpecId() == null ? 0L : dto.getSpecId();
        String field = field(dish.getId(), specId);
        int existing = currentQuantity(userId, field, dish.getId(), specId);
        int stock = stockService.currentStock(menu.getId(), menu.getStockLeft() == null ? 0 : menu.getStockLeft());
        int target = existing + dto.getQuantity();
        if (target > stock) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "库存不足，当前仅剩 " + Math.max(stock, 0) + " 份");
        }
        writeRedis(userId, field, target);
        upsertDb(userId, dish.getId(), specId, target);
        return getCart(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartVO update(Long userId, CartUpdateDTO dto) {
        Long specId = dto.getSpecId() == null ? 0L : dto.getSpecId();
        String field = field(dto.getDishId(), specId);
        int quantity = dto.getQuantity() == null ? 0 : dto.getQuantity();
        if (quantity == 0) {
            remove(userId, dto.getDishId(), specId);
            return getCart(userId);
        }
        Dish dish = dishService.requireDish(dto.getDishId());
        dishService.requireSpec(dish.getId(), specId);
        DailyMenu menu = menuService.findTodayMenu(dish.getId());
        if (menu == null) {
            throw new BizException(ResultCode.MENU_NOT_FOUND);
        }
        int stock = stockService.currentStock(menu.getId(), menu.getStockLeft() == null ? 0 : menu.getStockLeft());
        if (quantity > stock) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "库存不足，当前仅剩 " + Math.max(stock, 0) + " 份");
        }
        writeRedis(userId, field, quantity);
        upsertDb(userId, dish.getId(), specId, quantity);
        return getCart(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long dishId, Long specId) {
        Long normalizedSpec = specId == null ? 0L : specId;
        try {
            redis.<String, String>opsForHash().delete(RedisKeys.cart(userId), field(dishId, normalizedSpec));
        } catch (Exception e) {
            log.warn("Redis 删除购物车条目失败: {}", e.getMessage());
        }
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, dishId)
                .eq(CartItem::getSpecId, normalizedSpec));
    }

    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        try {
            redis.delete(RedisKeys.cart(userId));
        } catch (Exception e) {
            log.warn("Redis 清空购物车失败: {}", e.getMessage());
        }
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }

    /**
     * 下单使用：取原始条目，不组装价格。
     */
    public List<CartItem> loadRawItems(Long userId) {
        String key = RedisKeys.cart(userId);
        try {
            Map<String, String> entries = redis.<String, String>opsForHash().entries(key);
            if (!entries.isEmpty()) {
                List<CartItem> items = new ArrayList<>();
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    String[] parts = entry.getKey().split(":");
                    if (parts.length != 2) {
                        continue;
                    }
                    CartItem item = new CartItem();
                    item.setUserId(userId);
                    item.setDishId(Long.valueOf(parts[0]));
                    item.setSpecId(Long.valueOf(parts[1]));
                    item.setQuantity(Integer.valueOf(entry.getValue()));
                    if (item.getQuantity() > 0) {
                        items.add(item);
                    }
                }
                return items;
            }
        } catch (Exception e) {
            log.warn("Redis 读取购物车失败，回退数据库: {}", e.getMessage());
        }
        List<CartItem> dbItems = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId));
        if (!dbItems.isEmpty()) {
            rebuildCache(userId, dbItems);
        }
        return dbItems;
    }

    private int currentQuantity(Long userId, String field, Long dishId, Long specId) {
        try {
            String value = redis.<String, String>opsForHash().get(RedisKeys.cart(userId), field);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.warn("Redis 读取购物车数量失败: {}", e.getMessage());
        }
        CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, dishId)
                .eq(CartItem::getSpecId, specId));
        return item == null || item.getQuantity() == null ? 0 : item.getQuantity();
    }

    private void writeRedis(Long userId, String field, int quantity) {
        try {
            String key = RedisKeys.cart(userId);
            redis.<String, String>opsForHash().put(key, field, String.valueOf(quantity));
            redis.expire(key, CART_TTL);
        } catch (Exception e) {
            log.warn("Redis 写入购物车失败: {}", e.getMessage());
        }
    }

    private void upsertDb(Long userId, Long dishId, Long specId, int quantity) {
        CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, dishId)
                .eq(CartItem::getSpecId, specId));
        if (item == null) {
            item = new CartItem();
            item.setUserId(userId);
            item.setDishId(dishId);
            item.setSpecId(specId);
            item.setQuantity(quantity);
            cartItemMapper.insert(item);
        } else {
            item.setQuantity(quantity);
            cartItemMapper.updateById(item);
        }
    }

    private void rebuildCache(Long userId, List<CartItem> items) {
        try {
            String key = RedisKeys.cart(userId);
            for (CartItem item : items) {
                redis.<String, String>opsForHash().put(key, field(item.getDishId(), item.getSpecId()),
                        String.valueOf(item.getQuantity()));
            }
            redis.expire(key, CART_TTL);
        } catch (Exception e) {
            log.warn("回填 Redis 购物车失败: {}", e.getMessage());
        }
    }

    private Map<Long, Dish> loadDishes(List<CartItem> lines) {
        List<Long> ids = lines.stream().map(CartItem::getDishId).distinct()
                .collect(java.util.stream.Collectors.toList());
        List<Dish> dishes = dishService.listByIds(ids);
        Map<Long, Dish> map = new HashMap<>();
        dishes.forEach(d -> map.put(d.getId(), d));
        return map;
    }

    private Map<Long, DishSpec> loadSpecs(List<CartItem> lines) {
        List<Long> ids = lines.stream().map(CartItem::getSpecId)
                .filter(id -> id != null && id > 0).distinct()
                .collect(java.util.stream.Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        Map<Long, DishSpec> map = new HashMap<>();
        dishService.listSpecs(ids).forEach(s -> map.put(s.getId(), s));
        return map;
    }

    private BigDecimal unitPrice(Dish dish, DishSpec spec) {
        BigDecimal price = dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice();
        BigDecimal diff = spec == null || spec.getPriceDiff() == null ? BigDecimal.ZERO : spec.getPriceDiff();
        return price.add(diff).setScale(2, RoundingMode.HALF_UP);
    }

    private String field(Long dishId, Long specId) {
        return dishId + ":" + (specId == null ? 0L : specId);
    }
}
