package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.ConfigDTO;
import com.xatu.canteen.entity.SysConfig;
import com.xatu.canteen.entity.SysOperLog;
import com.xatu.canteen.entity.SysUser;
import com.xatu.canteen.mapper.SysConfigMapper;
import com.xatu.canteen.mapper.SysOperLogMapper;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.vo.ConfigVO;
import com.xatu.canteen.vo.OperLogVO;
import com.xatu.canteen.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 后台管理：用户、操作日志、参数配置。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserMapper userMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysConfigMapper configMapper;
    private final StaffStallService staffStallService;

    public PageResult<UserVO> pageUsers(String keyword, Integer pageNum, Integer pageSize) {
        Page<SysUser> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        userMapper.selectPage(page, Wrappers.<SysUser>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword))
                .orderByDesc(SysUser::getCreateTime));
        List<UserVO> vos = page.getRecords().stream().map(UserVO::from).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), vos);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserStatus(Long id, Integer status) {
        Long currentId = SecurityUtils.currentUserId();
        if (Objects.equals(currentId, id) && status != null && status == 0) {
            throw new BizException(ResultCode.ILLEGAL_STATE, "不能禁用当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    /**
     * 查看员工的档口权限。
     */
    public List<Long> listUserStalls(Long userId) {
        requireStaff(userId);
        return staffStallService.listBindings(userId);
    }

    /**
     * 整体替换员工的档口权限，空数组表示收回全部档口。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> updateUserStalls(Long userId, List<Long> stallIds) {
        requireStaff(userId);
        return staffStallService.replaceBindings(userId, stallIds);
    }

    private SysUser requireStaff(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!"STAFF".equals(user.getRoleCode())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能为档口员工分配档口权限");
        }
        return user;
    }

    public PageResult<OperLogVO> pageLogs(Integer pageNum, Integer pageSize) {
        Page<SysOperLog> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        operLogMapper.selectPage(page, Wrappers.<SysOperLog>lambdaQuery()
                .orderByDesc(SysOperLog::getCreateTime));
        List<OperLogVO> vos = page.getRecords().stream().map(OperLogVO::from).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), vos);
    }

    public List<ConfigVO> listConfigs() {
        return configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                        .orderByAsc(SysConfig::getConfigKey))
                .stream().map(ConfigVO::from).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ConfigVO saveConfig(ConfigDTO dto) {
        SysConfig exists = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, dto.getConfigKey()));
        if (exists == null) {
            exists = new SysConfig();
            exists.setConfigKey(dto.getConfigKey());
            exists.setConfigValue(dto.getConfigValue());
            exists.setRemark(dto.getRemark());
            configMapper.insert(exists);
        } else {
            exists.setConfigValue(dto.getConfigValue());
            exists.setRemark(dto.getRemark());
            configMapper.updateById(exists);
        }
        return ConfigVO.from(exists);
    }
}
