package com.power.workflow.controller;

import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.workflow.dto.TaskCompleteRequest;
import com.power.workflow.dto.TaskRejectRequest;
import com.power.workflow.dto.TaskTransferRequest;
import com.power.workflow.dto.TaskVO;
import com.power.workflow.service.WorkflowTaskAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务中心接口。
 */
@Tag(name = "任务中心", description = "待办、已办、认领与办理")
@RestController
@RequestMapping("/workflow/tasks")
@RequiredArgsConstructor
public class WorkflowTaskController {

    private final WorkflowTaskAppService workflowTaskAppService;

    /**
     * 我的待办分页。
     *
     * @param page 分页参数
     * @return 待办分页
     */
    @Operation(summary = "我的待办", description = "含 assignee 与候选人（用户/角色组）。权限码 workflow:task:list。")
    @GetMapping("/todo")
    @PreAuthorize("@authz.permit('workflow:task:list')")
    public R<PageResult<TaskVO>> todo(@Valid PageQuery page) {
        return R.ok(workflowTaskAppService.listTodo(page.getPageNum(), page.getPageSize()));
    }

    /**
     * 我的已办分页。
     *
     * @param page 分页参数
     * @return 已办分页
     */
    @Operation(summary = "我的已办", description = "当前用户已完成的历史任务。权限码 workflow:task:list。")
    @GetMapping("/done")
    @PreAuthorize("@authz.permit('workflow:task:list')")
    public R<PageResult<TaskVO>> done(@Valid PageQuery page) {
        return R.ok(workflowTaskAppService.listDone(page.getPageNum(), page.getPageSize()));
    }

    /**
     * 认领任务。
     *
     * @param taskId 任务 ID
     * @return 空成功响应
     */
    @Operation(summary = "认领任务", description = "候选人组任务认领到自己名下。权限码 workflow:task:handle。")
    @PostMapping("/{taskId}/claim")
    @PreAuthorize("@authz.permit('workflow:task:handle')")
    public R<Void> claim(@PathVariable String taskId) {
        workflowTaskAppService.claim(taskId);
        return R.ok();
    }

    /**
     * 取消认领。
     *
     * @param taskId 任务 ID
     * @return 空成功响应
     */
    @Operation(summary = "取消认领", description = "仅本人认领的任务可取消。权限码 workflow:task:handle。")
    @PostMapping("/{taskId}/unclaim")
    @PreAuthorize("@authz.permit('workflow:task:handle')")
    public R<Void> unclaim(@PathVariable String taskId) {
        workflowTaskAppService.unclaim(taskId);
        return R.ok();
    }

    /**
     * 完成任务。
     *
     * @param taskId  任务 ID
     * @param request 完成入参
     * @return 空成功响应
     */
    @Operation(summary = "完成任务", description = "可带 comment 与 variables；未认领时会先自动认领。默认 approved=true。权限码 workflow:task:handle。")
    @PostMapping("/{taskId}/complete")
    @PreAuthorize("@authz.permit('workflow:task:handle')")
    public R<Void> complete(@PathVariable String taskId, @RequestBody(required = false) TaskCompleteRequest request) {
        workflowTaskAppService.complete(taskId, request == null ? new TaskCompleteRequest() : request);
        return R.ok();
    }

    /**
     * 驳回任务。
     *
     * @param taskId  任务 ID
     * @param request 驳回入参
     * @return 空成功响应
     */
    @Operation(summary = "驳回任务", description = "有上一用户任务则退回；否则以不通过结束。权限码 workflow:task:handle")
    @PostMapping("/{taskId}/reject")
    @PreAuthorize("@authz.permit('workflow:task:handle')")
    public R<Void> reject(@PathVariable String taskId, @RequestBody(required = false) TaskRejectRequest request) {
        workflowTaskAppService.reject(taskId, request == null ? new TaskRejectRequest() : request);
        return R.ok();
    }

    /**
     * 转办任务。
     *
     * @param taskId  任务 ID
     * @param request 转办入参
     * @return 空成功响应
     */
    @Operation(summary = "转办任务", description = "将办理人改为 targetUserId。权限码 workflow:task:handle")
    @PostMapping("/{taskId}/transfer")
    @PreAuthorize("@authz.permit('workflow:task:handle')")
    public R<Void> transfer(@PathVariable String taskId, @Valid @RequestBody TaskTransferRequest request) {
        workflowTaskAppService.transfer(taskId, request);
        return R.ok();
    }
}
