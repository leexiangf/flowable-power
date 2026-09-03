package com.power.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.auth.domain.SysMenu;
import com.power.auth.domain.SysRole;
import com.power.auth.domain.SysRoleMenu;
import com.power.auth.domain.SysUserRole;
import com.power.auth.dto.RoleMenuAssignRequest;
import com.power.auth.dto.RoleSaveRequest;
import com.power.auth.dto.RoleVO;
import com.power.auth.mapper.SysMenuMapper;
import com.power.auth.mapper.SysRoleMapper;
import com.power.auth.mapper.SysRoleMenuMapper;
import com.power.auth.mapper.SysUserRoleMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色管理 CRUD 与菜单分配。
 */
@Service
@RequiredArgsConstructor
public class RoleAppService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final TokenSessionService tokenSessionService;

    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(RoleSaveRequest request) {
        assertRoleCodeAvailable(request.getRoleCode(), null);
        SysRole role = new SysRole();
        fillRole(role, request);
        sysRoleMapper.insert(role);
        return toVo(role, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleVO update(Long roleId, RoleSaveRequest request) {
        SysRole role = requireRole(roleId);
        assertRoleCodeAvailable(request.getRoleCode(), roleId);
        fillRole(role, request);
        sysRoleMapper.updateById(role);
        return detail(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long roleId) {
        requireRole(roleId);
        if (sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId)) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色仍有关联用户，无法删除");
        }
        sysRoleMapper.deleteById(roleId);
        sysRoleMenuMapper.deleteByRoleId(roleId);
    }

    public RoleVO detail(Long roleId) {
        SysRole role = requireRole(roleId);
        List<Long> menuIds = sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
        return toVo(role, menuIds);
    }

    public PageResult<RoleVO> page(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSort)
                .orderByDesc(SysRole::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysRole::getRoleCode, keyword).or().like(SysRole::getRoleName, keyword));
        }
        if (status != null) {
            qw.eq(SysRole::getStatus, status);
        }
        Page<SysRole> page = sysRoleMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<RoleVO> records = page.getRecords().stream()
                .map(r -> toVo(r, null))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<RoleVO> listEnabled() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getSort)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(r -> toVo(r, null))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, RoleMenuAssignRequest request) {
        requireRole(roleId);
        replaceRoleMenus(roleId, request.getMenuIds());
        bumpUsersByRole(roleId);
    }

    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<Long> distinct = menuIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long menuId : distinct) {
            SysMenu menu = sysMenuMapper.selectById(menuId);
            if (menu == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "菜单不存在: " + menuId);
            }
            SysRoleMenu link = new SysRoleMenu();
            link.setRoleId(roleId);
            link.setMenuId(menuId);
            sysRoleMenuMapper.insert(link);
        }
    }

    private void bumpUsersByRole(Long roleId) {
        List<Long> userIds = sysRoleMenuMapper.selectUserIdsByRoleId(roleId);
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            tokenSessionService.bumpUserVersion(userId);
        }
    }

    private void fillRole(SysRole role, RoleSaveRequest request) {
        role.setRoleCode(request.getRoleCode().trim());
        role.setRoleName(request.getRoleName().trim());
        role.setSort(request.getSort() == null ? 0 : request.getSort());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(trimToNull(request.getRemark()));
    }

    private RoleVO toVo(SysRole role, List<Long> menuIds) {
        return RoleVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .sort(role.getSort())
                .status(role.getStatus())
                .remark(role.getRemark())
                .menuIds(menuIds)
                .createTime(role.getCreateTime())
                .updateTime(role.getUpdateTime())
                .build();
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void assertRoleCodeAvailable(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode.trim());
        if (excludeId != null) {
            qw.ne(SysRole::getId, excludeId);
        }
        if (sysRoleMapper.selectCount(qw) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色编码已存在");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
