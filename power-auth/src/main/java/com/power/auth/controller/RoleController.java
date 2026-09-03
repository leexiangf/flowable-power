package com.power.auth.controller;

import com.power.auth.dto.RoleMenuAssignRequest;
import com.power.auth.dto.RoleSaveRequest;
import com.power.auth.dto.RoleVO;
import com.power.auth.service.RoleAppService;
import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
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
 * 角色管理接口。
 */
@Tag(name = "角色", description = "角色 CRUD 与菜单分配")
@RestController
@RequestMapping("/auth/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleAppService roleAppService;

    @Operation(summary = "角色分页", description = "权限码 system:role:list")
    @GetMapping
    @PreAuthorize("@authz.permit('system:role:list')")
    public R<PageResult<RoleVO>> page(@Valid PageQuery query,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(roleAppService.page(query.getPageNum(), query.getPageSize(), keyword, status));
    }

    @Operation(summary = "启用角色列表", description = "下拉选择；权限码 system:role:list")
    @GetMapping("/enabled")
    @PreAuthorize("@authz.permit('system:role:list')")
    public R<List<RoleVO>> listEnabled() {
        return R.ok(roleAppService.listEnabled());
    }

    @Operation(summary = "角色详情", description = "权限码 system:role:list；含 menuIds。")
    @GetMapping("/{roleId}")
    @PreAuthorize("@authz.permit('system:role:list')")
    public R<RoleVO> detail(@PathVariable Long roleId) {
        return R.ok(roleAppService.detail(roleId));
    }

    @Operation(summary = "新增角色", description = "权限码 system:role:add")
    @PostMapping
    @PreAuthorize("@authz.permit('system:role:add')")
    public R<RoleVO> create(@Valid @RequestBody RoleSaveRequest request) {
        return R.ok(roleAppService.create(request));
    }

    @Operation(summary = "修改角色", description = "权限码 system:role:edit")
    @PutMapping("/{roleId}")
    @PreAuthorize("@authz.permit('system:role:edit')")
    public R<RoleVO> update(@PathVariable Long roleId, @Valid @RequestBody RoleSaveRequest request) {
        return R.ok(roleAppService.update(roleId, request));
    }

    @Operation(summary = "删除角色", description = "权限码 system:role:remove")
    @DeleteMapping("/{roleId}")
    @PreAuthorize("@authz.permit('system:role:remove')")
    public R<Void> delete(@PathVariable Long roleId) {
        roleAppService.delete(roleId);
        return R.ok();
    }

    @Operation(summary = "分配角色菜单", description = "权限码 system:role:edit；覆盖式写入。")
    @PutMapping("/{roleId}/menus")
    @PreAuthorize("@authz.permit('system:role:edit')")
    public R<Void> assignMenus(@PathVariable Long roleId, @Valid @RequestBody RoleMenuAssignRequest request) {
        roleAppService.assignMenus(roleId, request);
        return R.ok();
    }
}
