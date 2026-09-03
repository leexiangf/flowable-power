package com.power.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.power.auth.domain.SysMenu;
import com.power.auth.dto.MenuVO;
import com.power.auth.mapper.SysMenuMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当前用户可见的路由 / 侧边栏菜单树。
 */
@Service
@RequiredArgsConstructor
public class MenuAppService {

    private final SysMenuMapper sysMenuMapper;

    /**
     * 构建当前用户的路由菜单树（仅目录 + 菜单，不含按钮）。
     *
     * @param userId 用户 ID
     * @return 树根列表
     */
    public List<MenuVO> treeForUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<SysMenu> assigned = sysMenuMapper.selectAssignedByUserId(userId);
        if (assigned.isEmpty()) {
            return List.of();
        }

        List<SysMenu> routerCandidates = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuType, 1, 2)
                .eq(SysMenu::getVisible, 1)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
        Map<Long, SysMenu> routerById = routerCandidates.stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a));

        Set<Long> includeIds = new HashSet<>();
        for (SysMenu menu : assigned) {
            Long startId = menu.getMenuType() != null && menu.getMenuType() == 3
                    ? menu.getParentId()
                    : menu.getId();
            addWithAncestors(startId, routerById, includeIds);
        }

        List<SysMenu> included = routerCandidates.stream()
                .filter(m -> includeIds.contains(m.getId()))
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenu::getId))
                .collect(Collectors.toList());
        return buildTree(included, 0L);
    }

    private void addWithAncestors(Long menuId, Map<Long, SysMenu> routerById, Set<Long> includeIds) {
        if (menuId == null || menuId <= 0) {
            return;
        }
        SysMenu menu = routerById.get(menuId);
        if (menu == null) {
            return;
        }
        includeIds.add(menuId);
        addWithAncestors(menu.getParentId(), routerById, includeIds);
    }

    private List<MenuVO> buildTree(List<SysMenu> menus, Long parentId) {
        List<MenuVO> result = new ArrayList<>();
        for (SysMenu menu : menus) {
            Long pid = menu.getParentId() == null ? 0L : menu.getParentId();
            if (!pid.equals(parentId)) {
                continue;
            }
            MenuVO vo = toVo(menu);
            vo.setChildren(buildTree(menus, menu.getId()));
            result.add(vo);
        }
        return result;
    }

    private MenuVO toVo(SysMenu menu) {
        return MenuVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .perms(menu.getPerms())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .build();
    }
}
