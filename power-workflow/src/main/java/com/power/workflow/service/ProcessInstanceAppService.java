package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.Authz;
import com.power.middleware.security.SecurityUtils;
import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.ProcessCancelRequest;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.ProcessStartRequest;
import com.power.workflow.dto.ProcessTerminateRequest;
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

import java.util.ArrayList;
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
    private final CcAppService ccAppService;
    private final Authz authz;

    /**
     * 按 key 启动流程实例，并写入发起人等变量。
     * <p>
     * 对外通用入口；leave / expense / 会签等专用业务请走对应 API（内部调用 {@link #startFromBusiness}）。
     */
    public ProcessInstanceVO start(ProcessStartRequest request) {
        return doStart(request, false);
    }

    /**
     * 业务专用入口启动（请假 / 报销 / 会签服务调用），跳过「请走专用接口」拦截。
     */
    public ProcessInstanceVO startFromBusiness(ProcessStartRequest request) {
        return doStart(request, true);
    }

    private ProcessInstanceVO doStart(ProcessStartRequest request, boolean fromBusinessApi) {
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
        assertStartVariables(request.getProcessDefinitionKey().trim(), request.getBusinessKey(), vars, fromBusinessApi);

        ProcessInstance pi = FlowableUserContext.callAs(userId, () ->
                runtimeService.startProcessInstanceByKey(
                        request.getProcessDefinitionKey().trim(),
                        request.getBusinessKey(),
                        vars));
        ccAppService.record(pi.getId(), null, request.getCcUserIds(), userId);
        return detail(pi.getId());
    }

    /**
     * 内置多实例 / 专用业务流：通用 start 必须带齐变量，否则引导走专用接口。
     */
    private void assertStartVariables(String processKey, String businessKey,
                                      Map<String, Object> vars, boolean fromBusinessApi) {
        if (ProcessKeys.LEAVE.equals(processKey)) {
            if (!fromBusinessApi) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "请假请使用「请假管理」或 POST /workflow/leave 发起");
            }
            if (!hasNonBlank(vars.get("leaveId"))) {
                throw new BizException(ErrorCode.BAD_REQUEST, "请假启动缺少 leaveId");
            }
            String leaveId = String.valueOf(vars.get("leaveId")).trim();
            if (!StringUtils.hasText(businessKey) || !leaveId.equals(businessKey.trim())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "请假 businessKey 必须与 leaveId 一致");
            }
            return;
        }
        if (ProcessKeys.EXPENSE.equals(processKey)) {
            if (!fromBusinessApi) {
                // 允许通用入口但必须变量齐全且办理人为审批人（防绕过专用校验）
                if (!hasNonBlank(vars.get(WorkflowVars.MANAGER_USER_ID))
                        || !hasNonEmptyCollection(vars.get(WorkflowVars.COUNTERSIGN_USER_IDS))) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "费用报销请使用「发起业务」或 POST /workflow/expense");
                }
            }
            if (!hasNonBlank(vars.get(WorkflowVars.MANAGER_USER_ID))) {
                throw new BizException(ErrorCode.BAD_REQUEST, "费用报销需 managerUserId");
            }
            if (!hasNonEmptyCollection(vars.get(WorkflowVars.COUNTERSIGN_USER_IDS))) {
                throw new BizException(ErrorCode.BAD_REQUEST, "费用报销需 countersignUserIds");
            }
            workflowIdentityFacade.assertOperatorUser(
                    String.valueOf(vars.get(WorkflowVars.MANAGER_USER_ID)).trim(), "部门经理");
            for (String uid : toUserIdList(vars.get(WorkflowVars.COUNTERSIGN_USER_IDS))) {
                workflowIdentityFacade.assertOperatorUser(uid, "会签人");
            }
            return;
        }
        if (ProcessKeys.COUNTERSIGN_OR.equals(processKey) || ProcessKeys.COUNTERSIGN_SEQ.equals(processKey)) {
            if (!fromBusinessApi && !hasNonEmptyCollection(vars.get(WorkflowVars.COUNTERSIGN_USER_IDS))) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "或签/会签请使用「发起业务」或 POST /workflow/countersign/or|seq");
            }
            List<String> users = toUserIdList(vars.get(WorkflowVars.COUNTERSIGN_USER_IDS));
            if (users.size() < 2) {
                throw new BizException(ErrorCode.BAD_REQUEST, "或签/会签至少需要 2 名审批人");
            }
            for (String uid : users) {
                workflowIdentityFacade.assertOperatorUser(uid, "会签人");
            }
        }
    }

    private static List<String> toUserIdList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    out.add(String.valueOf(item).trim());
                }
            }
        } else if (value != null && value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    out.add(String.valueOf(item).trim());
                }
            }
        }
        return out;
    }

    private static boolean hasNonBlank(Object value) {
        return value != null && StringUtils.hasText(String.valueOf(value));
    }

    private static boolean hasNonEmptyCollection(Object value) {
        if (value instanceof Iterable<?> iterable) {
            return iterable.iterator().hasNext();
        }
        if (value != null && value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) > 0;
        }
        return false;
    }

    /**
     * 分页查询当前用户发起的流程实例。
     */
    public PageResult<ProcessInstanceVO> listMine(long pageNum, long pageSize) {
        Long userId = requireLoginUserId();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .startedBy(String.valueOf(userId))
                .includeProcessVariables()
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
                .includeProcessVariables()
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
     * 查询流程实例详情（含变量）。仅发起可读权限、监控权限，或本人抄送接收人可查看。
     */
    public ProcessInstanceVO detail(String processInstanceId) {
        assertCanViewInstance(processInstanceId);
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        if (historic == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程实例不存在");
        }
        ProcessInstanceVO vo = toVo(historic);
        vo.setVariables(enrichVariablesForDisplay(vo.getVariables()));
        return vo;
    }

    /**
     * 实例可读性：list / monitor，或抄送接收人（须同时具备 task:cc）。
     */
    public void assertCanViewInstance(String processInstanceId) {
        Long userId = requireLoginUserId();
        if (authz.permit("workflow:instance:monitor") || authz.permit("workflow:instance:list")) {
            return;
        }
        if (authz.permit("workflow:task:cc") && ccAppService.isRecipient(processInstanceId, userId)) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权查看该流程实例");
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

    /**
     * 管理员强制终止运行中的流程实例。
     */
    @Transactional(rollbackFor = Exception.class)
    public void terminate(String processInstanceId, ProcessTerminateRequest request) {
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "流程已结束或不存在，无法终止");
        }
        String reason = request != null && StringUtils.hasText(request.getReason())
                ? request.getReason().trim()
                : "管理员强制终止";
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        if (ProcessKeys.LEAVE.equals(runtime.getProcessDefinitionKey())
                && StringUtils.hasText(runtime.getBusinessKey())) {
            leaveStatusUpdater.markCancelled(WorkflowApprovals.parseLongBusinessKey(runtime.getBusinessKey()));
        }
    }

    /**
     * 挂起运行中的流程实例。
     */
    public void suspend(String processInstanceId) {
        ProcessInstance runtime = requireRunningInstance(processInstanceId);
        if (runtime.isSuspended()) {
            return;
        }
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    /**
     * 激活已挂起的流程实例。
     */
    public void activate(String processInstanceId) {
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程实例不存在或已结束");
        }
        if (!runtime.isSuspended()) {
            return;
        }
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    private ProcessInstance requireRunningInstance(String processInstanceId) {
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程实例不存在或已结束");
        }
        return runtime;
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

    /**
     * 详情展示：用户 ID 类变量转为昵称，便于前端直接展示。
     */
    private Map<String, Object> enrichVariablesForDisplay(Map<String, Object> vars) {
        if (vars == null || vars.isEmpty()) {
            return vars;
        }
        Map<String, Object> out = new HashMap<>(vars);
        Object manager = vars.get(WorkflowVars.MANAGER_USER_ID);
        if (manager != null) {
            String name = workflowIdentityFacade.resolveDisplayName(String.valueOf(manager));
            if (StringUtils.hasText(name)) {
                out.put("managerUserName", name);
            }
        }
        Object countersign = vars.get(WorkflowVars.COUNTERSIGN_USER_IDS);
        if (countersign instanceof Iterable<?> iterable) {
            List<String> names = new ArrayList<>();
            for (Object id : iterable) {
                if (id == null) {
                    continue;
                }
                String name = workflowIdentityFacade.resolveDisplayName(String.valueOf(id));
                names.add(StringUtils.hasText(name) ? name : String.valueOf(id));
            }
            if (!names.isEmpty()) {
                out.put("countersignUserNames", names);
            }
        }
        return out;
    }

    private Long requireLoginUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
