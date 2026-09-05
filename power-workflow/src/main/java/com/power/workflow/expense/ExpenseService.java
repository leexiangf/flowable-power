package com.power.workflow.expense;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.ProcessStartRequest;
import com.power.workflow.dto.expense.ExpenseStartRequest;
import com.power.workflow.service.ProcessInstanceAppService;
import com.power.workflow.service.WorkflowIdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 费用报销：变量办理人 + 并行会签（无独立业务表，businessKey 使用 UUID）。
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ProcessInstanceAppService processInstanceAppService;
    private final WorkflowIdentityFacade workflowIdentityFacade;

    /**
     * 发起 expense 流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO start(ExpenseStartRequest request) {
        if (request == null || !StringUtils.hasText(request.getManagerUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "managerUserId 不能为空");
        }
        String managerUserId = request.getManagerUserId().trim();
        workflowIdentityFacade.assertOperatorUser(managerUserId, "部门经理");
        List<String> countersign = normalizeUserIds(request.getCountersignUserIds());
        if (countersign.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "会签人不能为空");
        }
        for (String uid : countersign) {
            workflowIdentityFacade.assertOperatorUser(uid, "会签人");
        }

        ProcessStartRequest start = new ProcessStartRequest();
        start.setProcessDefinitionKey(ProcessKeys.EXPENSE);
        start.setBusinessKey("expense-" + UUID.randomUUID().toString().replace("-", ""));
        if (StringUtils.hasText(request.getTitle())) {
            start.setTitle(request.getTitle().trim());
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put(WorkflowVars.BUSINESS_TYPE, WorkflowVars.BUSINESS_TYPE_EXPENSE);
        vars.put(WorkflowVars.MANAGER_USER_ID, managerUserId);
        vars.put(WorkflowVars.COUNTERSIGN_USER_IDS, countersign);
        if (StringUtils.hasText(request.getAmount())) {
            vars.put("amount", request.getAmount().trim());
        }
        if (StringUtils.hasText(request.getReason())) {
            vars.put("reason", request.getReason().trim());
        }
        start.setVariables(vars);
        return processInstanceAppService.startFromBusiness(start);
    }

    private List<String> normalizeUserIds(List<String> raw) {
        Set<String> set = new LinkedHashSet<>();
        if (raw == null) {
            return List.of();
        }
        for (String id : raw) {
            if (StringUtils.hasText(id)) {
                set.add(id.trim());
            }
        }
        return new ArrayList<>(set);
    }
}
