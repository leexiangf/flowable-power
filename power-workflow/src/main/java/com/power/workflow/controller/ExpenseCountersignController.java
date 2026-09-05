package com.power.workflow.controller;

import com.power.common.result.R;
import com.power.workflow.countersign.CountersignService;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.countersign.CountersignStartRequest;
import com.power.workflow.dto.expense.ExpenseStartRequest;
import com.power.workflow.expense.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 费用报销 / 会签业务发起。
 */
@Tag(name = "费用与会签", description = "费用报销、并行或签、串行会签")
@RestController
@RequiredArgsConstructor
public class ExpenseCountersignController {

    private final ExpenseService expenseService;
    private final CountersignService countersignService;

    @Operation(summary = "发起费用报销",
            description = "启动 expense。managerUserId 为变量办理人；countersignUserIds 为并行会签人。"
                    + "权限码 workflow:instance:start。")
    @PostMapping("/workflow/expense")
    @PreAuthorize("@authz.permit('workflow:instance:start')")
    public R<ProcessInstanceVO> startExpense(@Valid @RequestBody ExpenseStartRequest request) {
        return R.ok(expenseService.start(request));
    }

    @Operation(summary = "发起并行或签",
            description = "启动 countersign-or。权限码 workflow:instance:start。")
    @PostMapping("/workflow/countersign/or")
    @PreAuthorize("@authz.permit('workflow:instance:start')")
    public R<ProcessInstanceVO> startCountersignOr(@Valid @RequestBody CountersignStartRequest request) {
        return R.ok(countersignService.startOr(request));
    }

    @Operation(summary = "发起串行会签",
            description = "启动 countersign-seq。权限码 workflow:instance:start。")
    @PostMapping("/workflow/countersign/seq")
    @PreAuthorize("@authz.permit('workflow:instance:start')")
    public R<ProcessInstanceVO> startCountersignSeq(@Valid @RequestBody CountersignStartRequest request) {
        return R.ok(countersignService.startSeq(request));
    }
}
