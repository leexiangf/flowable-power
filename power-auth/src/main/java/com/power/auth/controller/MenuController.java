package com.power.auth.controller;

import com.power.auth.dto.MenuDetailVO;
import com.power.auth.dto.MenuSaveRequest;
import com.power.auth.dto.MenuVO;
import com.power.auth.service.MenuAppService;
import com.power.auth.service.MenuManageService;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.result.R;
import com.power.middleware.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单接口：当前用户路由树 + 管理端 CRUD。
 */
@Tag(name = "菜单", description = "动态侧边栏与菜单管理")
@RestController
@RequestMapping("/auth/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuAppService menuAppService;
    private final MenuManageService menuManageService;

    @Operation(summary = "当前用户菜单树", description = "供 Vue Router 动态注册；需登录，不含按钮节点。")
    @GetMapping("/tree")
    public R<List<MenuVO>> tree() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return R.ok(menuAppService.treeForUser(userId));
    }

    @Operation(summary = "管理端菜单树", description = "权限码 system:menu:list；含全部节点。")
    @GetMapping("/tree/all")
    @PreAuthorize("@authz.permit('system:menu:list')")
    public R<List<MenuDetailVO>> adminTree() {
        return R.ok(menuManageService.adminTree());
    }

    @Operation(summary = "菜单平铺列表", description = "权限码 system:menu:list")
    @GetMapping
    @PreAuthorize("@authz.permit('system:menu:list')")
    public R<List<MenuDetailVO>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(menuManageService.listFlat(keyword, status));
    }

    @Operation(summary = "菜单详情", description = "权限码 system:menu:list")
    @GetMapping("/{menuId}")
    @PreAuthorize("@authz.permit('system:menu:list')")
    public R<MenuDetailVO> detail(@PathVariable Long menuId) {
        return R.ok(menuManageService.detail(menuId));
    }

    @Operation(summary = "新增菜单", description = "权限码 system:menu:add")
    @PostMapping
    @PreAuthorize("@authz.permit('system:menu:add')")
    public R<MenuDetailVO> create(@Valid @RequestBody MenuSaveRequest request) {
        return R.ok(menuManageService.create(request));
    }

    @Operation(summary = "修改菜单", description = "权限码 system:menu:edit")
    @PutMapping("/{menuId}")
    @PreAuthorize("@authz.permit('system:menu:edit')")
    public R<MenuDetailVO> update(@PathVariable Long menuId, @Valid @RequestBody MenuSaveRequest request) {
        return R.ok(menuManageService.update(menuId, request));
    }

    @Operation(summary = "删除菜单", description = "权限码 system:menu:remove")
    @DeleteMapping("/{menuId}")
    @PreAuthorize("@authz.permit('system:menu:remove')")
    public R<Void> delete(@PathVariable Long menuId) {
        menuManageService.delete(menuId);
        return R.ok();
    }
}
