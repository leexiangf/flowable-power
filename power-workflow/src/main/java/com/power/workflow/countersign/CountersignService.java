package com.power.workflow.countersign;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.ProcessStartRequest;
import com.power.workflow.dto.countersign.CountersignStartRequest;
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
 * 会签业务：并行或签 / 串行会签。
 */
@Service
@RequiredArgsConstructor
public class CountersignService {

    private final ProcessInstanceAppService processInstanceAppService;
    private final WorkflowIdentityFacade workflowIdentityFacade;

    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startOr(CountersignStartRequest request) {
        return start(ProcessKeys.COUNTERSIGN_OR, WorkflowVars.BUSINESS_TYPE_COUNTERSIGN_OR, request);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startSeq(CountersignStartRequest request) {
        return start(ProcessKeys.COUNTERSIGN_SEQ, WorkflowVars.BUSINESS_TYPE_COUNTERSIGN_SEQ, request);
    }

    private ProcessInstanceVO start(
            String processKey,
            String businessType,
            CountersignStartRequest request) {
        List<String> users = normalizeUserIds(request == null ? null : request.getCountersignUserIds());
        if (users.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "会签人不能为空");
        }
        if (users.size() < 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, "至少选择 2 名会签人");
        }
        for (String uid : users) {
            workflowIdentityFacade.assertOperatorUser(uid, "会签人");
        }

        ProcessStartRequest start = new ProcessStartRequest();
        start.setProcessDefinitionKey(processKey);
        start.setBusinessKey(processKey + "-" + UUID.randomUUID().toString().replace("-", ""));
        if (request != null && StringUtils.hasText(request.getTitle())) {
            start.setTitle(request.getTitle().trim());
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put(WorkflowVars.BUSINESS_TYPE, businessType);
        vars.put(WorkflowVars.COUNTERSIGN_USER_IDS, users);
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
