package com.power.auth.controller;

import com.power.auth.dto.WorkflowUserVO;
import com.power.auth.service.WorkflowIdentityService;
import com.power.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流身份查询接口（assignee=userId，candidateGroups=roleCode）。
 * <p>
 * 登录用户即可调用（不绑 system:user:list），供待办候选组与昵称 enrichment。
 */
@Tag(name = "工作流身份", description = "供 Flowable 解析 assignee / candidateGroups；登录即可")
@RestController
@RequestMapping("/auth/workflow")
@RequiredArgsConstructor
public class WorkflowIdentityController {

    private final WorkflowIdentityService workflowIdentityService;

    @Operation(summary = "按用户 ID 查询", description = "返回用户基本信息（无密码）。需登录。")
    @GetMapping("/users/{userId}")
    @PreAuthorize("isAuthenticated()")
    public R<WorkflowUserVO> getUser(@PathVariable Long userId) {
        return R.ok(workflowIdentityService.getUser(userId));
    }

    @Operation(summary = "用户角色编码列表", description = "返回该用户启用中的 roleCode 列表。需登录。")
    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("isAuthenticated()")
    public R<List<String>> listRoleCodes(@PathVariable Long userId) {
        return R.ok(workflowIdentityService.listRoleCodes(userId));
    }

    @Operation(summary = "角色下用户 ID 列表", description = "按 roleCode 查询启用用户的 id。需登录。")
    @GetMapping("/roles/{roleCode}/user-ids")
    @PreAuthorize("isAuthenticated()")
    public R<List<Long>> listUserIdsByRole(@PathVariable String roleCode) {
        return R.ok(workflowIdentityService.listUserIdsByRoleCode(roleCode));
    }

    @Operation(summary = "角色下用户详情列表", description = "按 roleCode 返回用户详情（无密码）。需登录。")
    @GetMapping("/roles/{roleCode}/users")
    @PreAuthorize("isAuthenticated()")
    public R<List<WorkflowUserVO>> listUsersByRole(@PathVariable String roleCode) {
        return R.ok(workflowIdentityService.listUsersByRoleCode(roleCode));
    }
}
