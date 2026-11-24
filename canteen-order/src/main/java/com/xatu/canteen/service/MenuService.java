package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.CacheHelper;
import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.MenuPublishDTO;
import com.xatu.canteen.entity.DailyMenu;
import com.xatu.canteen.entity.Dish;
import com.xatu.canteen.entity.DishCategory;
import com.xatu.canteen.entity.Stall;
import com.xatu.canteen.mapper.DailyMenuMapper;
import com.xatu.canteen.mapper.DishCategoryMapper;
import com.xatu.canteen.mapper.DishMapper;
import com.xatu.canteen.mapper.StallMapper;
import com.xatu.canteen.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 每日菜单：发布、调整库存、今日菜单查询。菜单列表缓存 60 秒，实时库存叠加 Redis 值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final DailyMenuMapper dailyMenuMapper;
    private final DishMapper dishMapper;
    private final DishCategoryMapper categoryMapper;
    private final StallMapper stallMapper;
    private final StockService stockService;
    private final CacheHelper cacheHelper;

    /**
     * 今日可点菜单。缓存只缓存菜品与档口信息，库存每次从 Redis 叠加。
     */
    public List<MenuVO> todayList() {
        LocalDate today = LocalDate.now();
        String cacheKey = RedisKeys.todayMenu(today.toString());
        List<MenuVO> menus = cacheHelper.getList(cacheKey, MenuVO.class);
        if (menus == null) {
            menus = buildList(today, true);
            cacheHelper.set(cacheKey, menus, Duration.ofSeconds(60), true);
        }
        menus.forEach(menu -> menu.setStockLeft(
                stockService.currentStock(menu.getMenuId(), menu.getStockLeft() == null ? 0 : menu.getStockLeft())));
        return menus;
    }

    /**
     * 管理端查看某日菜单。
     */
    public List<MenuVO> listByDate(LocalDate date) {
        return buildList(date == null ? LocalDate.now() : date, false);
    }

    /**
     * 发布菜单：已存在的菜品跳过，库存取菜品每日默认库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public int publish(MenuPublishDTO dto) {
        LocalDate date = dto.getServeDate() == null ? LocalDate.now() : dto.getServeDate();
        List<Dish> dishes;
        if (CollectionUtils.isEmpty(dto.getDishIds())) {
            dishes = dishMapper.selectList(Wrappers.<Dish>lambdaQuery().eq(Dish::getStatus, 1));
        } else {
            dishes = dishMapper.selectBatchIds(dto.getDishIds()).stream()
                    .filter(d -> d.getStatus() != null && d.getStatus() == 1)
                    .collect(Collectors.toList());
        }
        int created = 0;
        for (Dish dish : dishes) {
            DailyMenu exists = dailyMenuMapper.selectOne(Wrappers.<DailyMenu>lambdaQuery()
                    .eq(DailyMenu::getDishId, dish.getId())
                    .eq(DailyMenu::getServeDate, date));
            if (exists != null) {
                stockService.initStock(exists.getId(), exists.getStockLeft() == null ? 0 : exists.getStockLeft());
                continue;
            }
            int stock = dish.getDailyStock() == null || dish.getDailyStock() <= 0 ? 50 : dish.getDailyStock();
            DailyMenu menu = new DailyMenu();
            menu.setDishId(dish.getId());
            menu.setServeDate(date);
            menu.setStockTotal(stock);
            menu.setStockLeft(stock);
            menu.setSales(0);
            menu.setStatus(1);
            dailyMenuMapper.insert(menu);
            stockService.initStock(menu.getId(), stock);
            created++;
        }
        cacheHelper.delete(RedisKeys.todayMenu(date.toString()));
        log.info("发布菜单 date={}, 新增 {} 条", date, created);
        return created;
    }

    /**
     * 调整总供应量，已售部分不变。
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuVO updateStock(Long menuId, Integer stockTotal) {
        DailyMenu menu = dailyMenuMapper.selectById(menuId);
        if (menu == null) {
            throw new BizException(ResultCode.MENU_NOT_FOUND);
        }
        int sold = menu.getSales() == null ? 0 : menu.getSales();
        int left = Math.max(0, stockTotal - sold);
        menu.setStockTotal(stockTotal);
        menu.setStockLeft(left);
        dailyMenuMapper.updateById(menu);
        stockService.resetStock(menuId, left);
        cacheHelper.delete(RedisKeys.todayMenu(menu.getServeDate().toString()));
        return toVO(menu, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * 启动/每日定时补齐今日菜单，已存在的跳过。
     */
    public void initTodayIfAbsent() {
        try {
            MenuPublishDTO dto = new MenuPublishDTO();
            dto.setServeDate(LocalDate.now());
            publish(dto);
        } catch (Exception e) {
            log.warn("初始化今日菜单失败: {}", e.getMessage());
        }
    }

    private List<MenuVO> buildList(LocalDate date, boolean onlyOnSale) {
        List<DailyMenu> menus = dailyMenuMapper.selectList(Wrappers.<DailyMenu>lambdaQuery()
                .eq(DailyMenu::getServeDate, date)
                .eq(onlyOnSale, DailyMenu::getStatus, 1)
                .orderByAsc(DailyMenu::getDishId));
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> dishIds = menus.stream().map(DailyMenu::getDishId).distinct().collect(Collectors.toList());
        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        List<Long> stallIds = dishes.stream().map(Dish::getStallId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        List<Long> categoryIds = dishes.stream().map(Dish::getCategoryId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, Dish> dishMap = dishes.stream().collect(Collectors.toMap(Dish::getId, d -> d, (a, b) -> a));
        Map<Long, String> stallNames = stallIds.isEmpty() ? Collections.emptyMap()
                : stallMapper.selectBatchIds(stallIds).stream()
                .collect(Collectors.toMap(Stall::getId, Stall::getName, (a, b) -> a));
        Map<Long, String> categoryNames = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(DishCategory::getId, DishCategory::getName, (a, b) -> a));

        List<MenuVO> result = new ArrayList<>();
        for (DailyMenu menu : menus) {
            Dish dish = dishMap.get(menu.getDishId());
            if (dish == null) {
                continue;
            }
            if (onlyOnSale && (dish.getStatus() == null || dish.getStatus() == 0)) {
                continue;
            }
            result.add(toVO(menu, dish, stallNames, categoryNames));
        }
        return result;
    }

    private MenuVO toVO(DailyMenu menu, Map<Long, String> stallNames, Map<Long, String> categoryNames) {
        Dish dish = menu.getDishId() == null ? null : dishMapper.selectById(menu.getDishId());
        return toVO(menu, dish, stallNames, categoryNames);
    }

    private MenuVO toVO(DailyMenu menu, Dish dish, Map<Long, String> stallNames, Map<Long, String> categoryNames) {
        MenuVO vo = new MenuVO();
        vo.setMenuId(menu.getId());
        vo.setDishId(menu.getDishId());
        vo.setStockTotal(menu.getStockTotal());
        vo.setStockLeft(menu.getStockLeft());
        vo.setSales(menu.getSales());
        vo.setServeDate(menu.getServeDate());
        vo.setStatus(menu.getStatus());
        if (dish != null) {
            vo.setDishName(dish.getName());
            vo.setCoverUrl(dish.getCoverUrl());
            vo.setPrice(dish.getPrice());
            vo.setCategoryId(dish.getCategoryId());
            vo.setStallId(dish.getStallId());
            vo.setCategoryName(categoryNames.get(dish.getCategoryId()));
            vo.setStallName(stallNames.get(dish.getStallId()));
        }
        return vo;
    }

    /**
     * 供购物车/下单校验：查询某菜品当日菜单。
     */
    public DailyMenu findTodayMenu(Long dishId) {
        return dailyMenuMapper.selectOne(Wrappers.<DailyMenu>lambdaQuery()
                .eq(DailyMenu::getDishId, dishId)
                .eq(DailyMenu::getServeDate, LocalDate.now())
                .eq(DailyMenu::getStatus, 1));
    }

    /**
     * 批量查询今日菜单，避免下单循环里逐条查库。
     */
    public Map<Long, DailyMenu> findTodayMenus(List<Long> dishIds) {
        if (CollectionUtils.isEmpty(dishIds)) {
            return Collections.emptyMap();
        }
        List<DailyMenu> menus = dailyMenuMapper.selectList(Wrappers.<DailyMenu>lambdaQuery()
                .in(DailyMenu::getDishId, dishIds)
                .eq(DailyMenu::getServeDate, LocalDate.now())
                .eq(DailyMenu::getStatus, 1));
        Map<Long, DailyMenu> map = new HashMap<>();
        menus.forEach(m -> map.put(m.getDishId(), m));
        return map;
    }
}
