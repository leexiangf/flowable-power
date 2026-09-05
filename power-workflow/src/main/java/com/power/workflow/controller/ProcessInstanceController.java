package com.power.workflow.controller;

import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.workflow.dto.ActivityTraceVO;
import com.power.workflow.dto.ProcessCancelRequest;
import com.power.workflow.dto.ProcessHighlightVO;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.ProcessStartRequest;
import com.power.workflow.dto.ProcessTerminateRequest;
import com.power.workflow.dto.ProcessUrgeRequest;
import com.power.workflow.dto.TaskVO;
import com.power.workflow.service.InstanceUrgeAppService;
import com.power.workflow.service.ProcessInstanceAppService;
import com.power.workflow.service.ProcessTraceAppService;
import com.power.workflow.service.WorkflowTaskAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程实例与追踪接口。
 */
@Tag(name = "流程实例", description = "启动实例、我发起的、详情、流转与流程图")
@RestController
@RequestMapping("/workflow/instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessInstanceAppService processInstanceAppService;
    private final ProcessTraceAppService processTraceAppService;
    private final InstanceUrgeAppService instanceUrgeAppService;
    private final WorkflowTaskAppService workflowTaskAppService;

    /**
     * 启动流程实例。
     *
     * @param request 启动入参
     * @return 实例视图
     */
    @Operation(summary = "启动流程实例", description = "按 processDefinitionKey 启动；自动写入 startUserId。权限码 workflow:instance:start。")
    @PostMapping("/start")
    @PreAuthorize("@authz.permit('workflow:instance:start')")
    public R<ProcessInstanceVO> start(@RequestBody ProcessStartRequest request) {
        return R.ok(processInstanceAppService.start(request));
    }

    /**
     * 我发起的流程实例分页。
     *
     * @param page 分页参数
     * @return 分页结果
     */
    @Operation(summary = "我发起的实例", description = "按当前登录用户查询历史/运行中实例。权限码 workflow:instance:list。")
    @GetMapping("/mine")
    @PreAuthorize("@authz.permit('workflow:instance:list')")
    public R<PageResult<ProcessInstanceVO>> mine(@Valid PageQuery page) {
        return R.ok(processInstanceAppService.listMine(page.getPageNum(), page.getPageSize()));
    }

    /**
     * 实例监控列表。
     *
     * @param page                 分页参数
     * @param processDefinitionKey 流程 key
     * @param finished             是否已结束
     * @return 分页结果
     */
    @Operation(summary = "实例监控列表", description = "管理员视角全量实例；权限码 workflow:instance:monitor")
    @GetMapping
    @PreAuthorize("@authz.permit('workflow:instance:monitor')")
    public R<PageResult<ProcessInstanceVO>> monitor(@Valid PageQuery page,
                                                    @RequestParam(required = false) String processDefinitionKey,
                                                    @RequestParam(required = false) Boolean finished) {
        return R.ok(processInstanceAppService.listMonitor(
                page.getPageNum(), page.getPageSize(), processDefinitionKey, finished));
    }

    /**
     * 流程实例详情。
     *
     * @param processInstanceId 实例 ID
     * @return 实例视图
     */
    @Operation(summary = "实例详情", description = "含状态、业务主键、标题与流程变量。权限码 workflow:instance:list。")
    @GetMapping("/{processInstanceId}")
    @PreAuthorize("@authz.permitAny('workflow:instance:list', 'workflow:instance:monitor', 'workflow:task:cc')")
    public R<ProcessInstanceVO> detail(@PathVariable String processInstanceId) {
        return R.ok(processInstanceAppService.detail(processInstanceId));
    }

    @Operation(summary = "当前活动任务",
            description = "实例下未完成用户任务，用于查看/重新指派。权限同实例详情。")
    @GetMapping("/{processInstanceId}/tasks")
    @PreAuthorize("@authz.permitAny('workflow:instance:list', 'workflow:instance:monitor', 'workflow:task:cc')")
    public R<List<TaskVO>> activeTasks(@PathVariable String processInstanceId) {
        return R.ok(workflowTaskAppService.listActiveByProcessInstance(processInstanceId));
    }

    /**
     * 发起人撤销流程。
     *
     * @param processInstanceId 实例 ID
     * @param request           撤销原因
     * @return 空成功响应
     */
    @Operation(summary = "撤销流程", description = "仅发起人可撤销运行中实例；权限码 workflow:instance:list")
    @PostMapping("/{processInstanceId}/cancel")
    @PreAuthorize("@authz.permit('workflow:instance:list')")
    public R<Void> cancel(@PathVariable String processInstanceId,
                          @RequestBody(required = false) ProcessCancelRequest request) {
        processInstanceAppService.cancel(processInstanceId, request == null ? new ProcessCancelRequest() : request);
        return R.ok();
    }

    @Operation(summary = "强制终止流程", description = "管理员终止运行中实例；权限码 workflow:instance:terminate")
    @PostMapping("/{processInstanceId}/terminate")
    @PreAuthorize("@authz.permit('workflow:instance:terminate')")
    public R<Void> terminate(@PathVariable String processInstanceId,
                             @RequestBody(required = false) ProcessTerminateRequest request) {
        processInstanceAppService.terminate(processInstanceId,
                request == null ? new ProcessTerminateRequest() : request);
        return R.ok();
    }

    @Operation(summary = "挂起流程实例", description = "权限码 workflow:instance:suspend")
    @PostMapping("/{processInstanceId}/suspend")
    @PreAuthorize("@authz.permit('workflow:instance:suspend')")
    public R<Void> suspendInstance(@PathVariable String processInstanceId) {
        processInstanceAppService.suspend(processInstanceId);
        return R.ok();
    }

    @Operation(summary = "激活流程实例", description = "权限码 workflow:instance:suspend")
    @PostMapping("/{processInstanceId}/activate")
    @PreAuthorize("@authz.permit('workflow:instance:suspend')")
    public R<Void> activateInstance(@PathVariable String processInstanceId) {
        processInstanceAppService.activate(processInstanceId);
        return R.ok();
    }

    @Operation(summary = "催办流程", description = "写入催办记录并投递 Outbox；权限码 workflow:task:urge")
    @PostMapping("/{processInstanceId}/urge")
    @PreAuthorize("@authz.permit('workflow:task:urge')")
    public R<Void> urge(@PathVariable String processInstanceId,
                        @RequestBody(required = false) ProcessUrgeRequest request) {
        instanceUrgeAppService.urge(processInstanceId, request == null ? new ProcessUrgeRequest() : request);
        return R.ok();
    }

    /**
     * 流转时间线。
     *
     * @param processInstanceId 实例 ID
     * @return 活动节点列表
     */
    @Operation(summary = "流转时间线", description = "历史活动节点 + 审批意见。权限码 workflow:instance:list。")
    @GetMapping("/{processInstanceId}/timeline")
    @PreAuthorize("@authz.permitAny('workflow:instance:list', 'workflow:instance:monitor', 'workflow:task:cc')")
    public R<List<ActivityTraceVO>> timeline(@PathVariable String processInstanceId) {
        return R.ok(processTraceAppService.timeline(processInstanceId));
    }

    /**
     * 流程图高亮数据。
     *
     * @param processInstanceId 实例 ID
     * @return 高亮视图
     */
    @Operation(summary = "流程图高亮数据", description = "返回 BPMN XML 与当前/已完成节点 id，供前端渲染高亮。权限码 workflow:instance:list。")
    @GetMapping("/{processInstanceId}/highlight")
    @PreAuthorize("@authz.permitAny('workflow:instance:list', 'workflow:instance:monitor', 'workflow:task:cc')")
    public R<ProcessHighlightVO> highlight(@PathVariable String processInstanceId) {
        return R.ok(processTraceAppService.highlight(processInstanceId));
    }

    /**
     * 流程图 PNG。
     *
     * @param processInstanceId 实例 ID
     * @return PNG 响应
     */
    @Operation(summary = "流程图 PNG", description = "服务端生成带高亮的流程图图片。权限码 workflow:instance:list。")
    @GetMapping("/{processInstanceId}/diagram")
    @PreAuthorize("@authz.permitAny('workflow:instance:list', 'workflow:instance:monitor', 'workflow:task:cc')")
    public ResponseEntity<byte[]> diagram(@PathVariable String processInstanceId) {
        byte[] png = processTraceAppService.diagramPng(processInstanceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"diagram.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
