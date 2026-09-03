package com.power.workflow.controller;

import com.power.common.result.R;
import com.power.workflow.demo.ExpenseDemoService;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.demo.ExpenseStartRequest;
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
 * M3 演示：变量办理人 + 会签。
 */
@Tag(name = "流程演示", description = "费用报销：变量办理人 + 并行会签")
@RestController
@RequestMapping("/workflow/demo")
@RequiredArgsConstructor
public class WorkflowDemoController {

    private final ExpenseDemoService expenseDemoService;

    @Operation(summary = "发起费用报销演示",
            description = "启动 expense 流程。managerUserId 为变量办理人；countersignUserIds 为并行会签人。"
                    + "权限码 workflow:instance:start。")
    @PostMapping("/expense")
    @PreAuthorize("@authz.permit('workflow:instance:start')")
    public R<ProcessInstanceVO> startExpense(@Valid @RequestBody ExpenseStartRequest request) {
        return R.ok(expenseDemoService.start(request));
    }
}
