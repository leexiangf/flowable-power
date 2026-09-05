package com.power.workflow.controller;

import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.workflow.dto.leave.LeaveCreateRequest;
import com.power.workflow.dto.leave.LeaveVO;
import com.power.workflow.leave.LeaveService;
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
 * 请假申请接口。
 */
@Tag(name = "请假", description = "请假申请业务（外置表单 + leave 流程）")
@RestController
@RequestMapping("/workflow/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    /**
     * 提交请假申请并启动审批流程。
     *
     * @param request 请假入参
     * @return 请假单视图
     */
    @Operation(summary = "提交请假申请", description = "写入 wf_leave 并启动 leave 流程，审批人为候选人组 APPROVER。权限码 workflow:leave:apply。")
    @PostMapping
    @PreAuthorize("@authz.permit('workflow:leave:apply')")
    public R<LeaveVO> create(@Valid @RequestBody LeaveCreateRequest request) {
        return R.ok(leaveService.create(request));
    }

    @Operation(summary = "我的请假列表", description = "分页查询当前用户请假申请。权限码 workflow:leave:list。")
    @GetMapping("/mine")
    @PreAuthorize("@authz.permit('workflow:leave:list')")
    public R<PageResult<LeaveVO>> mine(@Valid PageQuery page) {
        return R.ok(leaveService.listMine(page.getPageNum(), page.getPageSize()));
    }

    /**
     * 查询请假单详情。
     *
     * @param id 请假单 ID
     * @return 请假单视图
     */
    @Operation(summary = "请假单详情", description = "按业务主键查询请假单及关联流程实例 ID。权限码 workflow:leave:apply。")
    @GetMapping("/{id}")
    @PreAuthorize("@authz.permit('workflow:leave:apply')")
    public R<LeaveVO> detail(@PathVariable Long id) {
        return R.ok(leaveService.detail(id));
    }
}
