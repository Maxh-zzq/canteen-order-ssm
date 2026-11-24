package com.xatu.canteen.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.entity.StaffStall;
import com.xatu.canteen.entity.Stall;
import com.xatu.canteen.entity.SysUser;
import com.xatu.canteen.mapper.StaffStallMapper;
import com.xatu.canteen.mapper.StallMapper;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 启动初始化：保证三个演示账号存在，并补齐今日菜单。
 * 密码用 BCrypt 现算，避免在 SQL 里硬编码哈希。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final StaffStallMapper staffStallMapper;
    private final StallMapper stallMapper;
    private final PasswordEncoder passwordEncoder;
    private final MenuService menuService;

    @Override
    public void run(ApplicationArguments args) {
        ensureUser("admin", "系统管理员", "13800000000", "ADMIN", BigDecimal.ZERO);
        ensureUser("staff01", "一食堂员工", "13800000001", "STAFF", BigDecimal.ZERO);
        ensureUser("20230001", "张同学", "13800000002", "STUDENT", new BigDecimal("100.00"));
        ensureStaffStallBinding();
        menuService.initTodayIfAbsent();
        log.info("默认账号已就绪：admin/123456（管理员）、staff01/123456（档口）、20230001/123456（学生，余额 100）");
    }

    /**
     * 演示环境兜底：staff01 首次启动时默认绑定第一个档口。
     * 已存在任何绑定记录（包括管理员主动停用的记录）时不再自动修改，避免覆盖后台配置。
     */
    private void ensureStaffStallBinding() {
        SysUser staff = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, "staff01"));
        if (staff == null) {
            return;
        }
        Long count = staffStallMapper.selectCount(Wrappers.<StaffStall>lambdaQuery()
                .eq(StaffStall::getUserId, staff.getId()));
        if (count != null && count > 0) {
            return;
        }
        List<Stall> stalls = stallMapper.selectList(Wrappers.<Stall>lambdaQuery()
                .orderByAsc(Stall::getId));
        if (stalls.isEmpty()) {
            return;
        }
        StaffStall binding = new StaffStall();
        binding.setUserId(staff.getId());
        binding.setStallId(stalls.get(0).getId());
        binding.setStatus(1);
        staffStallMapper.insert(binding);
        log.info("已为 staff01 初始化档口权限: {}", stalls.get(0).getName());
    }

    private void ensureUser(String username, String realName, String phone, String role, BigDecimal balance) {
        SysUser exists = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (exists != null) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRoleCode(role);
        user.setBalance(balance);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("初始化账号: {} / 123456", username);
    }
}
