package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.SecurityUtils;
import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.ProcessCancelRequest;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.ProcessStartRequest;
import com.power.workflow.support.FlowableUserContext;
import com.power.workflow.support.WorkflowApprovals;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程实例：启动、我发起的、监控列表、撤销、详情。
 */
@Service
@RequiredArgsConstructor
public class ProcessInstanceAppService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ProcessDefinitionAppService processDefinitionAppService;
    private final WorkflowIdentityFacade workflowIdentityFacade;
    private final LeaveStatusUpdater leaveStatusUpdater;

    /**
     * 按 key 启动流程实例，并写入发起人等变量。
     */
    public ProcessInstanceVO start(ProcessStartRequest request) {
        Long userId = requireLoginUserId();
        if (request == null || !StringUtils.hasText(request.getProcessDefinitionKey())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "processDefinitionKey 不能为空");
        }
        processDefinitionAppService.requireLatestByKey(request.getProcessDefinitionKey());

        Map<String, Object> vars = new HashMap<>();
        if (request.getVariables() != null) {
            vars.putAll(request.getVariables());
        }
        vars.put(WorkflowVars.START_USER_ID, String.valueOf(userId));
        vars.put(WorkflowVars.START_USERNAME, SecurityUtils.currentUsername());
        if (StringUtils.hasText(request.getTitle())) {
            vars.put(WorkflowVars.TITLE, request.getTitle());
        }

        ProcessInstance pi = FlowableUserContext.callAs(userId, () ->
                runtimeService.startProcessInstanceByKey(
                        request.getProcessDefinitionKey().trim(),
                        request.getBusinessKey(),
                        vars));
        return detail(pi.getId());
    }

    /**
     * 分页查询当前用户发起的流程实例。
     */
    public PageResult<ProcessInstanceVO> listMine(long pageNum, long pageSize) {
        Long userId = requireLoginUserId();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .startedBy(String.valueOf(userId))
                .orderByProcessInstanceStartTime()
                .desc();
        return pageHistoric(query, pageNum, pageSize);
    }

    /**
     * 实例监控列表（管理员）：可按 key / 是否结束筛选。
     */
    public PageResult<ProcessInstanceVO> listMonitor(long pageNum, long pageSize,
                                                     String processDefinitionKey,
                                                     Boolean finished) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .orderByProcessInstanceStartTime()
                .desc();
        if (StringUtils.hasText(processDefinitionKey)) {
            query.processDefinitionKey(processDefinitionKey.trim());
        }
        if (Boolean.TRUE.equals(finished)) {
            query.finished();
        } else if (Boolean.FALSE.equals(finished)) {
            query.unfinished();
        }
        return pageHistoric(query, pageNum, pageSize);
    }

    /**
     * 查询流程实例详情（含变量）。
     */
    public ProcessInstanceVO detail(String processInstanceId) {
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        if (historic == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程实例不存在");
        }
        return toVo(historic);
    }

    /**
     * 发起人撤销运行中的流程实例。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String processInstanceId, ProcessCancelRequest request) {
        Long userId = requireLoginUserId();
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "流程已结束或不存在，无法撤销");
        }
        if (!Objects.equals(String.valueOf(userId), runtime.getStartUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅发起人可撤销");
        }
        String reason = request != null && StringUtils.hasText(request.getReason())
                ? request.getReason().trim()
                : "发起人撤销";
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        if (ProcessKeys.LEAVE.equals(runtime.getProcessDefinitionKey())
                && StringUtils.hasText(runtime.getBusinessKey())) {
            leaveStatusUpdater.markCancelled(WorkflowApprovals.parseLongBusinessKey(runtime.getBusinessKey()));
        }
    }

    private PageResult<ProcessInstanceVO> pageHistoric(HistoricProcessInstanceQuery query,
                                                       long pageNum, long pageSize) {
        long total = query.count();
        List<HistoricProcessInstance> list = query.listPage((int) ((pageNum - 1) * pageSize), (int) pageSize);
        List<ProcessInstanceVO> records = list.stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, total, pageNum, pageSize);
    }

    private ProcessInstanceVO toVo(HistoricProcessInstance h) {
        Map<String, Object> vars = h.getProcessVariables();
        if (vars == null) {
            HistoricProcessInstance withVars = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(h.getId())
                    .includeProcessVariables()
                    .singleResult();
            vars = withVars == null ? null : withVars.getProcessVariables();
        }
        Object title = vars == null ? null : vars.get(WorkflowVars.TITLE);
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(h.getId())
                .singleResult();
        return ProcessInstanceVO.builder()
                .id(h.getId())
                .processDefinitionId(h.getProcessDefinitionId())
                .processDefinitionKey(h.getProcessDefinitionKey())
                .processDefinitionName(h.getProcessDefinitionName())
                .businessKey(h.getBusinessKey())
                .startUserId(h.getStartUserId())
                .startUserName(workflowIdentityFacade.resolveDisplayName(h.getStartUserId()))
                .startTime(h.getStartTime())
                .endTime(h.getEndTime())
                .ended(h.getEndTime() != null)
                .suspended(runtime != null && runtime.isSuspended())
                .title(title == null ? null : String.valueOf(title))
                .variables(vars)
                .build();
    }

    private Long requireLoginUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
