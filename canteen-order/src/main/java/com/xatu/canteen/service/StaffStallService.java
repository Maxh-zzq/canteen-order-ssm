package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.entity.StaffStall;
import com.xatu.canteen.entity.Stall;
import com.xatu.canteen.entity.SysUser;
import com.xatu.canteen.mapper.StaffStallMapper;
import com.xatu.canteen.mapper.StallMapper;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.security.LoginUser;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.vo.StallVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 员工档口数据权限服务。
 *
 * <p>角色只回答“能不能调用员工接口”，本服务回答“能看到/操作哪些档口的订单”。
 * 员工看板、订单详情、接单、出餐、核销必须统一经过这里，不能只靠前端过滤。</p>
 */
@Service
@RequiredArgsConstructor
public class StaffStallService {

    private final StaffStallMapper staffStallMapper;
    private final StallMapper stallMapper;
    private final SysUserMapper userMapper;

    /**
     * 当前员工有效绑定的档口集合。ADMIN 不使用该集合，由调用方显式全量放行。
     */
    public Set<Long> activeStallIds(Long userId) {
        if (userId == null) {
            return new LinkedHashSet<>();
        }
        LocalDateTime now = LocalDateTime.now();
        return staffStallMapper.selectList(Wrappers.<StaffStall>lambdaQuery()
                        .eq(StaffStall::getUserId, userId)
                        .eq(StaffStall::getStatus, 1)
                        .and(w -> w.isNull(StaffStall::getEffectiveFrom)
                                .or().le(StaffStall::getEffectiveFrom, now))
                        .and(w -> w.isNull(StaffStall::getEffectiveTo)
                                .or().ge(StaffStall::getEffectiveTo, now)))
                .stream()
                .map(StaffStall::getStallId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 当前用户可见的档口。STAFF 返回绑定档口，ADMIN 返回全部档口。
     */
    public List<StallVO> myStalls() {
        LoginUser user = SecurityUtils.currentUser();
        List<Stall> stalls;
        if ("ADMIN".equals(user.getRole())) {
            stalls = stallMapper.selectList(Wrappers.<Stall>lambdaQuery()
                    .orderByAsc(Stall::getFloor)
                    .orderByAsc(Stall::getWindowNo));
        } else {
            Set<Long> allowed = activeStallIds(user.getId());
            if (allowed.isEmpty()) {
                return new ArrayList<>();
            }
            stalls = stallMapper.selectList(Wrappers.<Stall>lambdaQuery()
                    .in(Stall::getId, allowed)
                    .orderByAsc(Stall::getFloor)
                    .orderByAsc(Stall::getWindowNo));
        }
        return stalls.stream().map(StallVO::from).collect(Collectors.toList());
    }

    /**
     * 校验当前用户是否有权操作某个档口的订单。
     * ADMIN 显式全量放行；STAFF 必须在有效绑定集合内；其他角色一律拒绝。
     */
    public void assertStallAccess(Long stallId) {
        LoginUser user = SecurityUtils.currentUser();
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if (!"STAFF".equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        Set<Long> allowed = activeStallIds(user.getId());
        if (allowed.isEmpty()) {
            throw new BizException(ResultCode.STAFF_STALL_NOT_BOUND);
        }
        if (stallId == null || !allowed.contains(stallId)) {
            throw new BizException(ResultCode.ORDER_STALL_FORBIDDEN);
        }
    }

    /**
     * 管理员查看某个员工当前有效绑定的档口 ID。
     */
    public List<Long> listBindings(Long userId) {
        return activeBindings(userId).stream()
                .map(StaffStall::getStallId)
                .collect(Collectors.toList());
    }

    /**
     * 管理员为员工整体替换档口权限：请求内的启用，未请求的停用。
     * 保留历史绑定行，后续再次授权可直接复用，避免反复插入。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> replaceBindings(Long userId, List<Long> stallIds) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!"STAFF".equals(user.getRoleCode())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能为档口员工分配档口权限");
        }

        List<Long> requested = stallIds == null ? new ArrayList<>()
                : stallIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!requested.isEmpty()) {
            List<Stall> stalls = stallMapper.selectBatchIds(requested);
            if (stalls.size() != requested.size()) {
                throw new BizException(ResultCode.STALL_NOT_FOUND, "档口不存在或已删除");
            }
        }

        Map<Long, StaffStall> existing = staffStallMapper.selectList(Wrappers.<StaffStall>lambdaQuery()
                        .eq(StaffStall::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(StaffStall::getStallId, Function.identity(), (a, b) -> a));

        for (Long stallId : requested) {
            StaffStall binding = existing.get(stallId);
            if (binding == null) {
                StaffStall created = new StaffStall();
                created.setUserId(userId);
                created.setStallId(stallId);
                created.setStatus(1);
                staffStallMapper.insert(created);
            } else if (binding.getStatus() == null || binding.getStatus() != 1
                    || binding.getEffectiveTo() != null) {
                binding.setStatus(1);
                binding.setEffectiveFrom(null);
                binding.setEffectiveTo(null);
                staffStallMapper.updateById(binding);
            }
        }

        for (StaffStall binding : existing.values()) {
            if (!requested.contains(binding.getStallId())
                    && binding.getStatus() != null && binding.getStatus() == 1) {
                binding.setStatus(0);
                staffStallMapper.updateById(binding);
            }
        }
        return listBindings(userId);
    }

    public boolean hasAnyBinding(Long userId) {
        return staffStallMapper.selectCount(Wrappers.<StaffStall>lambdaQuery()
                .eq(StaffStall::getUserId, userId)) > 0;
    }

    private List<StaffStall> activeBindings(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return staffStallMapper.selectList(Wrappers.<StaffStall>lambdaQuery()
                .eq(StaffStall::getUserId, userId)
                .eq(StaffStall::getStatus, 1)
                .and(w -> w.isNull(StaffStall::getEffectiveFrom)
                        .or().le(StaffStall::getEffectiveFrom, now))
                .and(w -> w.isNull(StaffStall::getEffectiveTo)
                        .or().ge(StaffStall::getEffectiveTo, now))
                .orderByAsc(StaffStall::getStallId));
    }
}
