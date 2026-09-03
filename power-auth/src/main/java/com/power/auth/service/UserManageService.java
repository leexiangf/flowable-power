package com.power.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.auth.domain.SysRole;
import com.power.auth.domain.SysUser;
import com.power.auth.domain.SysUserRole;
import com.power.auth.dto.UserRoleAssignRequest;
import com.power.auth.dto.UserSaveRequest;
import com.power.auth.dto.UserVO;
import com.power.auth.mapper.SysRoleMapper;
import com.power.auth.mapper.SysUserMapper;
import com.power.auth.mapper.SysUserRoleMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户管理（管理员 CRUD、角色分配）。
 */
@Service
@RequiredArgsConstructor
public class UserManageService {

    private static final long PROTECTED_ADMIN_ID = 1L;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;

    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserSaveRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新增用户时 password 不能为空");
        }
        assertUsernameAvailable(request.getUsername(), null);
        SysUser user = new SysUser();
        fillUser(user, request, true);
        sysUserMapper.insert(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        tokenSessionService.bumpUserVersion(user.getId());
        return detail(user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Long userId, UserSaveRequest request) {
        SysUser user = requireUser(userId);
        assertUsernameAvailable(request.getUsername(), userId);
        fillUser(user, request, false);
        sysUserMapper.updateById(user);
        if (request.getRoleIds() != null) {
            replaceUserRoles(userId, request.getRoleIds());
            tokenSessionService.bumpUserVersion(userId);
        }
        return detail(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        if (Objects.equals(userId, PROTECTED_ADMIN_ID)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "内置管理员不可删除");
        }
        requireUser(userId);
        sysUserMapper.deleteById(userId);
        sysUserRoleMapper.deleteByUserId(userId);
        tokenSessionService.markUserDisabled(userId);
    }

    public UserVO detail(Long userId) {
        SysUser user = requireUser(userId);
        return toVo(user);
    }

    public PageResult<UserVO> page(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (status != null) {
            qw.eq(SysUser::getStatus, status);
        }
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<UserVO> records = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, UserRoleAssignRequest request) {
        requireUser(userId);
        replaceUserRoles(userId, request.getRoleIds());
        tokenSessionService.bumpUserVersion(userId);
    }

    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinct = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long roleId : distinct) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "角色不存在: " + roleId);
            }
            SysUserRole link = new SysUserRole();
            link.setUserId(userId);
            link.setRoleId(roleId);
            sysUserRoleMapper.insert(link);
        }
    }

    private void fillUser(SysUser user, UserSaveRequest request, boolean creating) {
        user.setUsername(request.getUsername().trim());
        if (creating || StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setNickname(trimToNull(request.getNickname()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setAvatar(trimToNull(request.getAvatar()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setRemark(trimToNull(request.getRemark()));
    }

    private UserVO toVo(SysUser user) {
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(user.getId());
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(user.getId());
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .remark(user.getRemark())
                .roleIds(roleIds == null ? Collections.emptyList() : roleIds)
                .roleCodes(roleCodes == null ? Collections.emptyList() : roleCodes)
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void assertUsernameAvailable(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim());
        if (excludeId != null) {
            qw.ne(SysUser::getId, excludeId);
        }
        if (sysUserMapper.selectCount(qw) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "登录名已存在");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
