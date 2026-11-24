package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.CacheHelper;
import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.CategoryDTO;
import com.xatu.canteen.dto.DishDTO;
import com.xatu.canteen.dto.DishSpecDTO;
import com.xatu.canteen.entity.Dish;
import com.xatu.canteen.entity.DishCategory;
import com.xatu.canteen.entity.DishSpec;
import com.xatu.canteen.entity.Stall;
import com.xatu.canteen.mapper.DishCategoryMapper;
import com.xatu.canteen.mapper.DishMapper;
import com.xatu.canteen.mapper.DishSpecMapper;
import com.xatu.canteen.mapper.StallMapper;
import com.xatu.canteen.vo.CategoryVO;
import com.xatu.canteen.vo.DishSpecVO;
import com.xatu.canteen.vo.DishVO;
import com.xatu.canteen.vo.StallVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
 * 菜品、分类、档口服务。公开查询走缓存，管理端写操作统一失效缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;
    private final DishCategoryMapper categoryMapper;
    private final DishSpecMapper specMapper;
    private final StallMapper stallMapper;
    private final CacheHelper cacheHelper;

    // ==================== 公开查询 ====================

    public List<CategoryVO> listCategories() {
        String key = RedisKeys.categoryList();
        List<CategoryVO> cached = cacheHelper.getList(key, CategoryVO.class);
        if (cached != null) {
            return cached;
        }
        List<DishCategory> categories = categoryMapper.selectList(Wrappers.<DishCategory>lambdaQuery()
                .eq(DishCategory::getStatus, 1)
                .orderByAsc(DishCategory::getSort)
                .orderByAsc(DishCategory::getId));
        List<CategoryVO> list = categories.stream().map(CategoryVO::from).collect(Collectors.toList());
        cacheHelper.set(key, list, Duration.ofMinutes(30), true);
        return list;
    }

    public PageResult<DishVO> pageDishes(Long categoryId, Long stallId, String keyword,
                                         Integer pageNum, Integer pageSize) {
        Page<Dish> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        IPage<Dish> result = dishMapper.selectPage(page, Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getStatus, 1)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .eq(stallId != null, Dish::getStallId, stallId)
                .like(StringUtils.hasText(keyword), Dish::getName, keyword)
                .orderByDesc(Dish::getSales)
                .orderByAsc(Dish::getId));
        List<DishVO> vos = toVOs(result.getRecords());
        return PageResult.of(result.getTotal(), result.getPages(), result.getCurrent(), result.getSize(), vos);
    }

    /**
     * 管理端菜品分页：包含已停用菜品，并返回规格，便于编辑与上下架。
     */
    public PageResult<DishVO> pageAllDishes(Long categoryId, Long stallId, String keyword,
                                            Integer pageNum, Integer pageSize) {
        Page<Dish> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        IPage<Dish> result = dishMapper.selectPage(page, Wrappers.<Dish>lambdaQuery()
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .eq(stallId != null, Dish::getStallId, stallId)
                .like(StringUtils.hasText(keyword), Dish::getName, keyword)
                .orderByDesc(Dish::getSales)
                .orderByAsc(Dish::getId));
        List<DishVO> vos = toVOs(result.getRecords(), true);
        return PageResult.of(result.getTotal(), result.getPages(), result.getCurrent(), result.getSize(), vos);
    }

    /**
     * 管理端菜品详情：允许查看已停用菜品。
     */
    public DishVO adminDetail(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        return toVO(dish);
    }

    /**
     * 管理端分类列表：包含停用分类，按排序值展示。
     */
    public List<CategoryVO> listAllCategories() {
        return categoryMapper.selectList(Wrappers.<DishCategory>lambdaQuery()
                        .orderByAsc(DishCategory::getSort)
                        .orderByAsc(DishCategory::getId))
                .stream().map(CategoryVO::from).collect(Collectors.toList());
    }

    public DishVO detail(Long id) {
        String cacheKey = RedisKeys.dishDetail(id);
        DishVO cached = cacheHelper.get(cacheKey, DishVO.class);
        if (cached != null) {
            return cached;
        }
        if (cacheHelper.exists(RedisKeys.dishNull(id))) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        Dish dish = dishMapper.selectById(id);
        if (dish == null || dish.getStatus() == null || dish.getStatus() == 0) {
            // 空值缓存 60 秒，防止缓存穿透
            cacheHelper.set(RedisKeys.dishNull(id), "1", Duration.ofSeconds(60));
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        DishVO vo = toVO(dish);
        cacheHelper.set(cacheKey, vo, Duration.ofMinutes(30), true);
        return vo;
    }

    public List<StallVO> listStalls() {
        return stallMapper.selectList(Wrappers.<Stall>lambdaQuery()
                        .eq(Stall::getStatus, 1)
                        .orderByAsc(Stall::getFloor)
                        .orderByAsc(Stall::getWindowNo))
                .stream().map(StallVO::from).collect(Collectors.toList());
    }

    /**
     * 管理端档口列表：包含已停用档口。
     */
    public List<StallVO> listAllStalls() {
        return stallMapper.selectList(Wrappers.<Stall>lambdaQuery()
                        .orderByAsc(Stall::getFloor)
                        .orderByAsc(Stall::getWindowNo))
                .stream().map(StallVO::from).collect(Collectors.toList());
    }

    // ==================== 分类管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public CategoryVO createCategory(CategoryDTO dto) {
        DishCategory category = new DishCategory();
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        categoryMapper.insert(category);
        cacheHelper.delete(RedisKeys.categoryList());
        return CategoryVO.from(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryVO updateCategory(Long id, CategoryDTO dto) {
        DishCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        categoryMapper.updateById(category);
        cacheHelper.delete(RedisKeys.categoryList());
        cacheHelper.delete(RedisKeys.todayMenu(LocalDate.now().toString()));
        return CategoryVO.from(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        long dishes = dishMapper.selectCount(Wrappers.<Dish>lambdaQuery().eq(Dish::getCategoryId, id));
        if (dishes > 0) {
            throw new BizException(ResultCode.ILLEGAL_STATE, "该分类下还有 " + dishes + " 个菜品，无法删除");
        }
        categoryMapper.deleteById(id);
        cacheHelper.delete(RedisKeys.categoryList());
    }

    // ==================== 菜品管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public DishVO createDish(DishDTO dto) {
        requireStall(dto.getStallId());
        requireCategory(dto.getCategoryId());
        Dish dish = new Dish();
        apply(dish, dto);
        dish.setSales(0);
        dish.setAvgScore(BigDecimal.ZERO);
        dishMapper.insert(dish);
        saveSpecs(dish.getId(), dto.getSpecs());
        evictDishCache(dish.getId());
        return toVO(dish);
    }

    @Transactional(rollbackFor = Exception.class)
    public DishVO updateDish(Long id, DishDTO dto) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        requireStall(dto.getStallId());
        requireCategory(dto.getCategoryId());
        apply(dish, dto);
        dishMapper.updateById(dish);
        if (dto.getSpecs() != null) {
            specMapper.delete(Wrappers.<DishSpec>lambdaQuery().eq(DishSpec::getDishId, id));
            saveSpecs(id, dto.getSpecs());
        }
        evictDishCache(id);
        return toVO(dish);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDish(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        dishMapper.deleteById(id);
        specMapper.delete(Wrappers.<DishSpec>lambdaQuery().eq(DishSpec::getDishId, id));
        evictDishCache(id);
    }

    // ==================== 档口管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public StallVO createStall(Stall stall) {
        if (!StringUtils.hasText(stall.getName())) {
            throw new BizException(ResultCode.BAD_REQUEST, "档口名称不能为空");
        }
        stall.setId(null);
        stall.setStatus(stall.getStatus() == null ? 1 : stall.getStatus());
        stallMapper.insert(stall);
        return StallVO.from(stall);
    }

    @Transactional(rollbackFor = Exception.class)
    public StallVO updateStall(Long id, Stall stall) {
        Stall exists = stallMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.NOT_FOUND, "档口不存在");
        }
        exists.setName(stall.getName());
        exists.setFloor(stall.getFloor());
        exists.setWindowNo(stall.getWindowNo());
        exists.setManagerId(stall.getManagerId());
        exists.setOpenTime(stall.getOpenTime());
        exists.setCloseTime(stall.getCloseTime());
        if (stall.getStatus() != null) {
            exists.setStatus(stall.getStatus());
        }
        stallMapper.updateById(exists);
        cacheHelper.delete(RedisKeys.todayMenu(LocalDate.now().toString()));
        return StallVO.from(exists);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStall(Long id) {
        long dishes = dishMapper.selectCount(Wrappers.<Dish>lambdaQuery().eq(Dish::getStallId, id));
        if (dishes > 0) {
            throw new BizException(ResultCode.ILLEGAL_STATE, "该档口下还有 " + dishes + " 个菜品，无法删除");
        }
        stallMapper.deleteById(id);
    }

    // ==================== 内部方法 ====================

    public List<Dish> listByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return dishMapper.selectBatchIds(ids);
    }

    public List<DishSpec> listSpecs(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return specMapper.selectBatchIds(ids);
    }

    public Dish requireDish(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        if (dish.getStatus() == null || dish.getStatus() == 0) {
            throw new BizException(ResultCode.DISH_OFF_SALE);
        }
        return dish;
    }

    public DishSpec requireSpec(Long dishId, Long specId) {
        if (specId == null || specId == 0L) {
            return null;
        }
        DishSpec spec = specMapper.selectById(specId);
        if (spec == null || spec.getStatus() == null || spec.getStatus() == 0
                || !Objects.equals(spec.getDishId(), dishId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "菜品规格不存在或已停用");
        }
        return spec;
    }

    private void apply(Dish dish, DishDTO dto) {
        dish.setStallId(dto.getStallId());
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName());
        dish.setCoverUrl(dto.getCoverUrl());
        dish.setPrice(dto.getPrice());
        dish.setDescription(dto.getDescription());
        dish.setDailyStock(dto.getDailyStock() == null ? 50 : dto.getDailyStock());
        dish.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private void saveSpecs(Long dishId, List<DishSpecDTO> specs) {
        if (CollectionUtils.isEmpty(specs)) {
            return;
        }
        for (DishSpecDTO dto : specs) {
            DishSpec spec = new DishSpec();
            spec.setDishId(dishId);
            spec.setName(dto.getName());
            spec.setPriceDiff(dto.getPriceDiff() == null ? BigDecimal.ZERO : dto.getPriceDiff());
            spec.setStatus(1);
            specMapper.insert(spec);
        }
    }

    private void requireStall(Long stallId) {
        if (stallMapper.selectById(stallId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "档口不存在");
        }
    }

    private void requireCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }
    }

    private List<DishVO> toVOs(List<Dish> dishes) {
        return toVOs(dishes, false);
    }

    private List<DishVO> toVOs(List<Dish> dishes, boolean withSpecs) {
        if (dishes.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> stallNames = stallMapper.selectList(null).stream()
                .collect(Collectors.toMap(Stall::getId, Stall::getName, (a, b) -> a));
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(DishCategory::getId, DishCategory::getName, (a, b) -> a));
        List<DishVO> list = new ArrayList<>(dishes.size());
        for (Dish dish : dishes) {
            DishVO vo = toVO(dish, stallNames, categoryNames, withSpecs);
            list.add(vo);
        }
        return list;
    }

    private DishVO toVO(Dish dish) {
        Map<Long, String> stallNames = Collections.emptyMap();
        Map<Long, String> categoryNames = Collections.emptyMap();
        if (dish.getStallId() != null) {
            Stall stall = stallMapper.selectById(dish.getStallId());
            if (stall != null) {
                stallNames = Collections.singletonMap(stall.getId(), stall.getName());
            }
        }
        if (dish.getCategoryId() != null) {
            DishCategory category = categoryMapper.selectById(dish.getCategoryId());
            if (category != null) {
                categoryNames = Collections.singletonMap(category.getId(), category.getName());
            }
        }
        return toVO(dish, stallNames, categoryNames, true);
    }

    private DishVO toVO(Dish dish, Map<Long, String> stallNames, Map<Long, String> categoryNames, boolean withSpecs) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setStallId(dish.getStallId());
        vo.setStallName(stallNames.get(dish.getStallId()));
        vo.setCategoryId(dish.getCategoryId());
        vo.setCategoryName(categoryNames.get(dish.getCategoryId()));
        vo.setName(dish.getName());
        vo.setCoverUrl(dish.getCoverUrl());
        vo.setPrice(dish.getPrice());
        vo.setDescription(dish.getDescription());
        vo.setDailyStock(dish.getDailyStock());
        vo.setSales(dish.getSales());
        vo.setAvgScore(dish.getAvgScore());
        vo.setStatus(dish.getStatus());
        if (withSpecs) {
            List<DishSpec> specs = specMapper.selectList(Wrappers.<DishSpec>lambdaQuery()
                    .eq(DishSpec::getDishId, dish.getId())
                    .eq(DishSpec::getStatus, 1)
                    .orderByAsc(DishSpec::getId));
            vo.setSpecs(specs.stream().map(DishSpecVO::from).collect(Collectors.toList()));
        }
        return vo;
    }

    private void evictDishCache(Long dishId) {
        cacheHelper.delete(RedisKeys.dishDetail(dishId));
        cacheHelper.delete(RedisKeys.dishNull(dishId));
        cacheHelper.delete(RedisKeys.todayMenu(LocalDate.now().toString()));
    }
}
