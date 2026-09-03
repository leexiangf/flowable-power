package com.power.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.power.auth.domain.SysMenu;
import com.power.auth.dto.MenuDetailVO;
import com.power.auth.dto.MenuSaveRequest;
import com.power.auth.mapper.SysMenuMapper;
import com.power.auth.mapper.SysRoleMenuMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.constant.SystemConstants;
import com.power.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理 CRUD（管理员）。
 */
@Service
@RequiredArgsConstructor
public class MenuManageService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Transactional(rollbackFor = Exception.class)
    public MenuDetailVO create(MenuSaveRequest request) {
        validateMenuRequest(request, null);
        SysMenu menu = new SysMenu();
        fillMenu(menu, request);
        sysMenuMapper.insert(menu);
        return toVo(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuDetailVO update(Long menuId, MenuSaveRequest request) {
        assertNotBuiltInMenu(menuId);
        SysMenu menu = requireMenu(menuId);
        validateMenuRequest(request, menuId);
        fillMenu(menu, request);
        sysMenuMapper.updateById(menu);
        return toVo(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long menuId) {
        assertNotBuiltInMenu(menuId);
        requireMenu(menuId);
        long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在子菜单，无法删除");
        }
        sysMenuMapper.deleteById(menuId);
        sysRoleMenuMapper.deleteByMenuId(menuId);
    }

    public MenuDetailVO detail(Long menuId) {
        return toVo(requireMenu(menuId));
    }

    public List<MenuDetailVO> listFlat(String keyword, Integer status) {
        LambdaQueryWrapper<SysMenu> qw = new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysMenu::getMenuName, keyword).or().like(SysMenu::getPerms, keyword));
        }
        if (status != null) {
            qw.eq(SysMenu::getStatus, status);
        }
        return sysMenuMapper.selectList(qw).stream().map(this::toVo).collect(Collectors.toList());
    }

    public List<MenuDetailVO> adminTree() {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
        return buildTree(menus, 0L);
    }

    private void validateMenuRequest(MenuSaveRequest request, Long selfId) {
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        if (parentId > 0) {
            SysMenu parent = sysMenuMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "父菜单不存在");
            }
            if (parent.getMenuType() != null && parent.getMenuType() == 3) {
                throw new BizException(ErrorCode.BAD_REQUEST, "按钮下不能再挂子节点");
            }
            if (SystemConstants.isBuiltInMenu(parentId)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "内置菜单下不可新增子菜单");
            }
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "父节点不能是自己");
        }
        int type = request.getMenuType();
        if (type == 3 && !StringUtils.hasText(request.getPerms())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "按钮类型必须填写权限码");
        }
    }

    private void fillMenu(SysMenu menu, MenuSaveRequest request) {
        menu.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        menu.setMenuName(request.getMenuName().trim());
        menu.setMenuType(request.getMenuType());
        menu.setPath(trimToNull(request.getPath()));
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setPerms(trimToNull(request.getPerms()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setVisible(request.getVisible() == null ? 1 : request.getVisible());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        menu.setRemark(trimToNull(request.getRemark()));
    }

    private List<MenuDetailVO> buildTree(List<SysMenu> menus, Long parentId) {
        List<MenuDetailVO> result = new ArrayList<>();
        for (SysMenu menu : menus) {
            Long pid = menu.getParentId() == null ? 0L : menu.getParentId();
            if (!pid.equals(parentId)) {
                continue;
            }
            MenuDetailVO vo = toVo(menu);
            vo.setChildren(buildTree(menus, menu.getId()));
            result.add(vo);
        }
        result.sort(Comparator.comparing(MenuDetailVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuDetailVO::getId));
        return result;
    }

    private MenuDetailVO toVo(SysMenu menu) {
        return MenuDetailVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .perms(menu.getPerms())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .remark(menu.getRemark())
                .builtIn(SystemConstants.isBuiltInMenu(menu.getId()))
                .createTime(menu.getCreateTime())
                .updateTime(menu.getUpdateTime())
                .build();
    }

    private void assertNotBuiltInMenu(Long menuId) {
        if (SystemConstants.isBuiltInMenu(menuId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "内置菜单不可修改或删除");
        }
    }

    private SysMenu requireMenu(Long menuId) {
        SysMenu menu = sysMenuMapper.selectById(menuId);
        if (menu == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        return menu;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
