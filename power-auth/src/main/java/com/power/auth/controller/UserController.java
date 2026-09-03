package com.power.auth.controller;

import com.power.auth.dto.CurrentUserVO;
import com.power.auth.dto.PasswordChangeRequest;
import com.power.auth.dto.ProfileUpdateRequest;
import com.power.auth.dto.UserRoleAssignRequest;
import com.power.auth.dto.UserSaveRequest;
import com.power.auth.dto.UserVO;
import com.power.auth.service.AuthService;
import com.power.auth.service.UserAppService;
import com.power.auth.service.UserManageService;
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

/**
 * 用户接口：当前用户、用户管理。
 */
@Tag(name = "用户", description = "当前用户信息与用户管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserAppService userAppService;
    private final UserManageService userManageService;

    @Operation(summary = "当前登录用户", description = "返回资料、roleCode 列表与权限码；权限从库实时加载。")
    @GetMapping("/me")
    public R<CurrentUserVO> me() {
        return R.ok(userAppService.currentProfile());
    }

    @Operation(summary = "更新个人资料", description = "仅修改当前用户 nickname/email/phone/avatar。")
    @PutMapping("/me/profile")
    public R<CurrentUserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.ok(userAppService.updateProfile(request));
    }

    @Operation(summary = "修改密码", description = "校验原密码后更新；成功后旧 Token 失效。")
    @PutMapping("/me/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userAppService.changePassword(request);
        return R.ok();
    }

    @Operation(summary = "用户分页列表", description = "权限码 system:user:list；响应中不含密码。")
    @GetMapping("/users")
    @PreAuthorize("@authz.permit('system:user:list')")
    public R<PageResult<UserVO>> users(PageQuery query,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(userManageService.page(query.getPageNum(), query.getPageSize(), keyword, status));
    }

    @Operation(summary = "用户详情", description = "权限码 system:user:list")
    @GetMapping("/users/{userId}")
    @PreAuthorize("@authz.permit('system:user:list')")
    public R<UserVO> userDetail(@PathVariable Long userId) {
        return R.ok(userManageService.detail(userId));
    }

    @Operation(summary = "新增用户", description = "权限码 system:user:add")
    @PostMapping("/users")
    @PreAuthorize("@authz.permit('system:user:add')")
    public R<UserVO> createUser(@Valid @RequestBody UserSaveRequest request) {
        return R.ok(userManageService.create(request));
    }

    @Operation(summary = "修改用户", description = "权限码 system:user:edit；password 留空表示不改。")
    @PutMapping("/users/{userId}")
    @PreAuthorize("@authz.permit('system:user:edit')")
    public R<UserVO> updateUser(@PathVariable Long userId, @Valid @RequestBody UserSaveRequest request) {
        return R.ok(userManageService.update(userId, request));
    }

    @Operation(summary = "删除用户", description = "权限码 system:user:remove；逻辑删除。")
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("@authz.permit('system:user:remove')")
    public R<Void> deleteUser(@PathVariable Long userId) {
        userManageService.delete(userId);
        return R.ok();
    }

    @Operation(summary = "分配用户角色", description = "权限码 system:user:edit")
    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("@authz.permit('system:user:edit')")
    public R<Void> assignRoles(@PathVariable Long userId, @Valid @RequestBody UserRoleAssignRequest request) {
        userManageService.assignRoles(userId, request);
        return R.ok();
    }

    @Operation(summary = "禁用用户", description = "权限码 system:user:edit；禁用后该用户 Token 立即失效。")
    @PostMapping("/users/{userId}/disable")
    @PreAuthorize("@authz.permit('system:user:edit')")
    public R<Void> disable(@PathVariable Long userId) {
        authService.disableUser(userId);
        return R.ok();
    }

    @Operation(summary = "启用用户", description = "权限码 system:user:edit。")
    @PostMapping("/users/{userId}/enable")
    @PreAuthorize("@authz.permit('system:user:edit')")
    public R<Void> enable(@PathVariable Long userId) {
        authService.enableUser(userId);
        return R.ok();
    }
}
