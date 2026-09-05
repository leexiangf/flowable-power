package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.Authz;
import com.power.middleware.security.SecurityUtils;
import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.TaskAddSignRequest;
import com.power.workflow.dto.TaskCompleteRequest;
import com.power.workflow.dto.TaskDelegateRequest;
import com.power.workflow.dto.TaskRejectRequest;
import com.power.workflow.dto.TaskTransferRequest;
import com.power.workflow.dto.TaskVO;
import com.power.workflow.dto.UserTaskNodeVO;
import com.power.workflow.support.FlowableUserContext;
import com.power.workflow.support.TaskAddSignSupport;
import com.power.workflow.support.WorkflowApprovals;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务中心：待办/已办查询，认领与完成。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskAppService {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final WorkflowIdentityFacade workflowIdentityFacade;
    private final CcAppService ccAppService;
    private final TaskAddSignSupport taskAddSignSupport;
    private final LeaveStatusUpdater leaveStatusUpdater;
    private final ProcessInstanceAppService processInstanceAppService;
    private final Authz authz;

    /**
     * 当前用户待办（办理人 + 候选人/候选人组）。
     * <p>
     * 顺带自愈：无办理人且设计办理人是当前用户的孤儿任务，自动恢复 assignee。
     */
    public PageResult<TaskVO> listTodo(long pageNum, long pageSize) {
        Long userId = requireLoginUserId();
        String userIdStr = String.valueOf(userId);
        healOrphanTasksForUser(userIdStr);
        List<String> roleCodes = workflowIdentityFacade.listRoleCodes(userId);

        // 含委派 owner：PENDING 时 assignee 是被委派人，owner 也需在待办中看到「收回」
        TaskQuery query = taskService.createTaskQuery().active().orderByTaskCreateTime().desc();
        if (roleCodes == null || roleCodes.isEmpty()) {
            query.or()
                    .taskAssignee(userIdStr)
                    .taskCandidateUser(userIdStr)
                    .taskOwner(userIdStr)
                    .endOr();
        } else {
            query.or()
                    .taskAssignee(userIdStr)
                    .taskCandidateUser(userIdStr)
                    .taskCandidateGroupIn(roleCodes)
                    .taskOwner(userIdStr)
                    .endOr();
        }
        long total = query.count();
        List<Task> tasks = query.listPage((int) ((pageNum - 1) * pageSize), (int) pageSize);
        List<TaskVO> records = tasks.stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 当前用户已办任务。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 已办分页
     */
    public PageResult<TaskVO> listDone(long pageNum, long pageSize) {
        Long userId = requireLoginUserId();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(userId))
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        long total = query.count();
        List<HistoricTaskInstance> tasks = query.listPage((int) ((pageNum - 1) * pageSize), (int) pageSize);
        List<TaskVO> records = tasks.stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 认领候选人任务到当前用户。
     *
     * @param taskId 任务 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void claim(String taskId) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanClaim(task, userId);
        FlowableUserContext.runAs(userId, () -> taskService.claim(taskId, String.valueOf(userId)));
    }

    /**
     * 取消本人已认领的任务（仅候选人认领场景）。
     * <p>
     * 变量指定办理人任务（如 expense 的 managerUserId）禁止取消认领，
     * 否则 assignee 被清空且无候选人，任务将对所有人不可见。
     */
    @Transactional(rollbackFor = Exception.class)
    public void unclaim(String taskId) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        if (!Objects.equals(String.valueOf(userId), task.getAssignee())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能取消自己认领的任务");
        }
        if (task.getDelegationState() != null && "PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "委派中的任务不可取消认领，请使用归还");
        }
        if (taskAddSignSupport.isAddSignPending(taskId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "加签中的任务不可取消认领");
        }
        if (!hasCandidateLinks(taskId)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "指定办理人任务不可取消认领，如需交他人办理请使用「转办」");
        }
        FlowableUserContext.runAs(userId, () -> taskService.unclaim(taskId));
    }

    /**
     * 重新指派办理人（修复无办理人的孤儿任务，或管理员改派）。
     * 权限：任务办理人 / 设计办理人 / 发起人 / 实例监控员。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(String taskId, TaskTransferRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        if (request == null || !StringUtils.hasText(request.getTargetUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 不能为空");
        }
        String targetUserId = request.getTargetUserId().trim();
        assertCanAssign(task, userId);
        workflowIdentityFacade.assertOperatorUser(targetUserId, "指派目标");
        FlowableUserContext.runAs(userId, () -> {
            if (StringUtils.hasText(request.getComment())) {
                taskService.addComment(taskId, task.getProcessInstanceId(), request.getComment().trim());
            }
            taskService.setAssignee(taskId, targetUserId);
        });
    }

    /**
     * 列出流程实例下当前活动用户任务（用于详情页重新指派）。
     */
    public List<TaskVO> listActiveByProcessInstance(String processInstanceId) {
        processInstanceAppService.assertCanViewInstance(processInstanceId);
        requireLoginUserId();
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .orderByTaskCreateTime()
                .asc()
                .list();
        return tasks.stream().map(this::toVo).collect(Collectors.toList());
    }

    /**
     * 完成任务：可写入意见与变量；未认领时先自动认领。
     *
     * @param taskId  任务 ID
     * @param request 完成入参（可为 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(String taskId, TaskCompleteRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        // Flowable：PENDING 委派任务禁止 complete，须先 resolve 归还给 owner
        if (task.getDelegationState() != null && "PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "委派中的任务请先「归还」，由原办理人完成");
        }

        String comment = request != null ? request.getComment() : null;
        // 前加签：加签人办结归还原办理人，不推进节点
        Boolean resolved = FlowableUserContext.callAs(userId, () ->
                taskAddSignSupport.tryResolveBeforeAddSign(task, userId, comment));
        if (Boolean.TRUE.equals(resolved)) {
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        if (request != null && request.getVariables() != null) {
            vars.putAll(request.getVariables());
        }
        // 缺省通过；若客户端传入字符串/数字则规范为 Boolean，避免网关条件失败
        boolean approved = WorkflowApprovals.toBoolean(vars.get(WorkflowVars.APPROVED), true);
        boolean multiInstance = isMultiInstanceTask(task);
        // 会签：任一实例已写入 false 后不可被后续 complete 覆盖为 true
        if (multiInstance) {
            Object existing = runtimeService.getVariable(task.getProcessInstanceId(), WorkflowVars.APPROVED);
            if (!WorkflowApprovals.toBoolean(existing, true)) {
                approved = false;
            }
        }
        vars.put(WorkflowVars.APPROVED, approved);

        FlowableUserContext.runAs(userId, () -> {
            if (task.getAssignee() == null) {
                taskService.claim(taskId, String.valueOf(userId));
            }
            if (StringUtils.hasText(comment)) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment.trim());
            }
            // 后加签推进时清理局部变量
            if (taskAddSignSupport.isAddSignPending(taskId)) {
                taskAddSignSupport.clearAddSignLocals(taskId);
            }
            taskService.complete(taskId, vars);
        });
        if (!approved && multiInstance) {
            cancelRemainingMultiInstanceSiblings(
                    task.getProcessInstanceId(), task.getTaskDefinitionKey(), taskId);
        }
        if (request != null) {
            ccAppService.record(task.getProcessInstanceId(), taskId, request.getCcUserIds(), userId);
        }
    }

    /**
     * 驳回：
     * <ul>
     *   <li>TERMINATE：结束流程（approved=false）</li>
     *   <li>TO_STARTER / TO_NODE / PREVIOUS：退回指定或上一用户任务</li>
     *   <li>多实例：默认以 approved=false 完成当前实例</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, TaskRejectRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        if (task.getDelegationState() != null && "PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "委派中的任务请先「归还」，由原办理人驳回");
        }
        if (taskAddSignSupport.isAddSignPending(taskId)
                && TaskAddSignSupport.TYPE_BEFORE.equals(
                String.valueOf(taskService.getVariableLocal(taskId, WorkflowVars.ADD_SIGN_MODE)))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "前加签中请先由加签人归还再驳回");
        }
        String comment = request != null && StringUtils.hasText(request.getComment())
                ? request.getComment().trim()
                : "驳回";
        String strategy = request != null && StringUtils.hasText(request.getStrategy())
                ? request.getStrategy().trim().toUpperCase()
                : "PREVIOUS";

        boolean multiInstance = isMultiInstanceTask(task);
        if ("TERMINATE".equals(strategy)) {
            String processInstanceId = task.getProcessInstanceId();
            String businessKey = null;
            String defKey = null;
            HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (historic != null) {
                businessKey = historic.getBusinessKey();
                defKey = historic.getProcessDefinitionKey();
            }
            String finalBusinessKey = businessKey;
            String finalDefKey = defKey;
            FlowableUserContext.runAs(userId, () -> {
                if (task.getAssignee() == null) {
                    taskService.claim(taskId, String.valueOf(userId));
                }
                taskService.addComment(taskId, processInstanceId, comment);
                runtimeService.setVariable(processInstanceId, WorkflowVars.APPROVED, false);
                runtimeService.deleteProcessInstance(processInstanceId, comment);
            });
            if (ProcessKeys.LEAVE.equals(finalDefKey) && StringUtils.hasText(finalBusinessKey)) {
                leaveStatusUpdater.markFinished(WorkflowApprovals.parseLongBusinessKey(finalBusinessKey), false);
            }
            return;
        }

        String targetActivityId = null;
        if (!multiInstance) {
            if ("TO_STARTER".equals(strategy)) {
                targetActivityId = findFirstUserTaskActivityId(task);
            } else if ("TO_NODE".equals(strategy)) {
                targetActivityId = request != null ? request.getTargetActivityId() : null;
                if (!StringUtils.hasText(targetActivityId)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "TO_NODE 须指定 targetActivityId");
                }
            } else {
                targetActivityId = request != null ? request.getTargetActivityId() : null;
                if (!StringUtils.hasText(targetActivityId)) {
                    targetActivityId = findPreviousUserTaskActivityId(task);
                }
            }
        }

        String finalTarget = targetActivityId;
        FlowableUserContext.runAs(userId, () -> {
            if (task.getAssignee() == null) {
                taskService.claim(taskId, String.valueOf(userId));
            }
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            runtimeService.setVariable(task.getProcessInstanceId(), WorkflowVars.APPROVED, false);
            if (taskAddSignSupport.isAddSignPending(taskId)) {
                taskAddSignSupport.clearAddSignLocals(taskId);
            }
            if (StringUtils.hasText(finalTarget)) {
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(task.getProcessInstanceId())
                        .moveActivityIdTo(task.getTaskDefinitionKey(), finalTarget)
                        .changeState();
            } else {
                Map<String, Object> vars = new HashMap<>();
                vars.put(WorkflowVars.APPROVED, false);
                taskService.complete(taskId, vars);
            }
        });
        if (multiInstance && !StringUtils.hasText(finalTarget)) {
            cancelRemainingMultiInstanceSiblings(
                    task.getProcessInstanceId(), task.getTaskDefinitionKey(), taskId);
        }
    }

    /**
     * 加签（前/后，指定一人）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, TaskAddSignRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        if (request == null || !StringUtils.hasText(request.getTargetUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 不能为空");
        }
        workflowIdentityFacade.assertOperatorUser(request.getTargetUserId().trim(), "加签目标");
        FlowableUserContext.runAs(userId, () -> taskAddSignSupport.addSign(task, userId, request));
    }

    /**
     * 减签：删除当前多实例（会签）子执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reduceSign(String taskId) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        if (!isMultiInstanceTask(task)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅会签（多实例）任务可减签");
        }
        Object active = runtimeService.getVariable(task.getExecutionId(), "nrOfActiveInstances");
        long activeCount = active instanceof Number ? ((Number) active).longValue() : 0L;
        if (activeCount <= 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "会签仅剩一人，不可减签；请办理或驳回");
        }
        FlowableUserContext.runAs(userId, () -> {
            taskService.addComment(taskId, task.getProcessInstanceId(), "减签");
            runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), false);
        });
    }

    /**
     * 列出本流程定义中可作为驳回目标的用户任务节点（排除当前节点）。
     */
    public List<UserTaskNodeVO> listRejectableNodes(String taskId) {
        Task task = requireActiveTask(taskId);
        requireLoginUserId();
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) {
            return List.of();
        }
        Map<String, UserTaskNodeVO> nodes = new LinkedHashMap<>();
        for (FlowElement element : model.getMainProcess().getFlowElements()) {
            if (element instanceof UserTask userTask) {
                if (Objects.equals(userTask.getId(), task.getTaskDefinitionKey())) {
                    continue;
                }
                // 禁止驳回到多实例节点，避免 changeActivityState 进入半成品会签
                if (userTask.getLoopCharacteristics() != null) {
                    continue;
                }
                nodes.put(userTask.getId(), UserTaskNodeVO.builder()
                        .activityId(userTask.getId())
                        .activityName(userTask.getName())
                        .build());
            }
        }
        return new ArrayList<>(nodes.values());
    }

    /**
     * 转办给其他用户。
     *
     * @param taskId  任务 ID
     * @param request 转办入参
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String taskId, TaskTransferRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        if (task.getDelegationState() != null && "PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "委派中的任务请先归还再转办");
        }
        if (request == null || !StringUtils.hasText(request.getTargetUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 不能为空");
        }
        String targetUserId = request.getTargetUserId().trim();
        if (Objects.equals(String.valueOf(userId), targetUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能转办给自己");
        }
        workflowIdentityFacade.assertOperatorUser(targetUserId, "转办目标");
        FlowableUserContext.runAs(userId, () -> {
            if (task.getAssignee() == null) {
                taskService.claim(taskId, String.valueOf(userId));
            }
            if (StringUtils.hasText(request.getComment())) {
                taskService.addComment(taskId, task.getProcessInstanceId(), request.getComment().trim());
            }
            taskService.setAssignee(taskId, targetUserId);
        });
    }

    /**
     * 委派任务：原办理人保留 owner，assignee 改为被委派人。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, TaskDelegateRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        if (task.getDelegationState() != null && "PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务已在委派中，请先归还");
        }
        if (request == null || !StringUtils.hasText(request.getTargetUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 不能为空");
        }
        String targetUserId = request.getTargetUserId().trim();
        if (Objects.equals(String.valueOf(userId), targetUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能委派给自己");
        }
        workflowIdentityFacade.assertOperatorUser(targetUserId, "委派目标");
        FlowableUserContext.runAs(userId, () -> {
            if (task.getAssignee() == null) {
                taskService.claim(taskId, String.valueOf(userId));
            }
            if (StringUtils.hasText(request.getComment())) {
                taskService.addComment(taskId, task.getProcessInstanceId(), request.getComment().trim());
            }
            taskService.delegateTask(taskId, targetUserId);
        });
        ccAppService.record(task.getProcessInstanceId(), taskId, request.getCcUserIds(), userId);
    }

    /**
     * Resolve 委派：被委派人或 owner 可将任务归还给 owner（delegationState → RESOLVED）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resolveDelegate(String taskId) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        String userIdStr = String.valueOf(userId);
        boolean isOwner = Objects.equals(userIdStr, task.getOwner());
        boolean isAssignee = Objects.equals(userIdStr, task.getAssignee());
        if (!isOwner && !isAssignee) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅委派发起人或被委派人可归还");
        }
        if (task.getDelegationState() == null || !"PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务不在委派中");
        }
        FlowableUserContext.runAs(userId, () -> taskService.resolveTask(taskId));
    }

    private String findPreviousUserTaskActivityId(Task task) {
        List<HistoricActivityInstance> finished = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .desc()
                .list();
        for (HistoricActivityInstance act : finished) {
            if (!Objects.equals(act.getActivityId(), task.getTaskDefinitionKey())) {
                return act.getActivityId();
            }
        }
        return null;
    }

    /** 发起后的第一个用户任务节点（用于 TO_STARTER）。 */
    private String findFirstUserTaskActivityId(Task task) {
        List<HistoricActivityInstance> started = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        if (!started.isEmpty()) {
            String first = started.get(0).getActivityId();
            // 已在首个用户任务：无法再退回，返回 null 走 complete(approved=false)
            if (!Objects.equals(first, task.getTaskDefinitionKey())) {
                return first;
            }
            return null;
        }
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) {
            return null;
        }
        for (FlowElement element : model.getMainProcess().getFlowElements()) {
            if (element instanceof UserTask userTask
                    && !Objects.equals(userTask.getId(), task.getTaskDefinitionKey())) {
                return userTask.getId();
            }
        }
        return null;
    }

    /**
     * 多实例（会签）子任务：存在 loopCounter 局部变量。
     */
    private boolean isMultiInstanceTask(Task task) {
        try {
            return taskService.getVariableLocal(task.getId(), "loopCounter") != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 一票否决后取消同节点其余多实例子任务（兼容旧版或签 BPMN 未含 approved==false 完成条件）。
     */
    private void cancelRemainingMultiInstanceSiblings(
            String processInstanceId, String taskDefinitionKey, String excludeTaskId) {
        if (!StringUtils.hasText(processInstanceId) || !StringUtils.hasText(taskDefinitionKey)) {
            return;
        }
        List<Task> siblings = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .active()
                .list();
        if (siblings == null || siblings.isEmpty()) {
            return;
        }
        for (Task sibling : siblings) {
            if (excludeTaskId != null && excludeTaskId.equals(sibling.getId())) {
                continue;
            }
            try {
                runtimeService.deleteMultiInstanceExecution(sibling.getExecutionId(), false);
            } catch (Exception ignored) {
                // 子执行可能已被完成条件清理
            }
        }
    }

    private String localString(String taskId, String name) {
        try {
            Object v = taskService.getVariableLocal(taskId, name);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ex) {
            return null;
        }
    }

    private void assertCanClaim(Task task, Long userId) {
        if (task.getAssignee() != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务已被认领");
        }
        if (!isCandidate(task, userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权认领该任务");
        }
    }

    private void assertCanComplete(Task task, Long userId) {
        String userIdStr = String.valueOf(userId);
        if (userIdStr.equals(task.getAssignee())) {
            return;
        }
        if (task.getAssignee() == null && isCandidate(task, userId)) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权办理该任务");
    }

    private void assertCanAssign(Task task, Long userId) {
        String userIdStr = String.valueOf(userId);
        if (userIdStr.equals(task.getAssignee())) {
            return;
        }
        if (authz.permit("workflow:instance:monitor")) {
            return;
        }
        String designed = resolveDesignedAssignee(task);
        if (userIdStr.equals(designed)) {
            return;
        }
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (historic != null && userIdStr.equals(historic.getStartUserId())) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权重新指派该任务");
    }

    private boolean isCandidate(Task task, Long userId) {
        List<String> roleCodes = workflowIdentityFacade.listRoleCodes(userId);
        TaskQuery query = taskService.createTaskQuery().taskId(task.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            query.taskCandidateUser(String.valueOf(userId));
        } else {
            query.or()
                    .taskCandidateUser(String.valueOf(userId))
                    .taskCandidateGroupIn(roleCodes)
                    .endOr();
        }
        return query.count() > 0;
    }

    /** 是否存在候选人/组（可安全 unclaim 回到候选人池）。 */
    private boolean hasCandidateLinks(String taskId) {
        List<IdentityLink> links = taskService.getIdentityLinksForTask(taskId);
        if (links == null || links.isEmpty()) {
            return false;
        }
        for (IdentityLink link : links) {
            if ("candidate".equals(link.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 BPMN/变量设计的办理人（用于孤儿任务自愈与指派授权）。
     */
    private String resolveDesignedAssignee(Task task) {
        try {
            Object localAssignee = taskService.getVariableLocal(task.getId(), "assignee");
            if (localAssignee != null && StringUtils.hasText(String.valueOf(localAssignee))) {
                return String.valueOf(localAssignee).trim();
            }
        } catch (Exception ignored) {
            // ignore
        }
        String key = task.getTaskDefinitionKey();
        String formKey = task.getFormKey();
        if ("managerTask".equals(key) || "expense/manager".equals(formKey)) {
            Object manager = runtimeService.getVariable(
                    task.getProcessInstanceId(), WorkflowVars.MANAGER_USER_ID);
            if (manager != null && StringUtils.hasText(String.valueOf(manager))) {
                return String.valueOf(manager).trim();
            }
        }
        return null;
    }

    /** 当前用户是设计办理人的未分配任务：恢复 assignee，避免永久消失。 */
    private void healOrphanTasksForUser(String userIdStr) {
        List<Task> unassigned = taskService.createTaskQuery()
                .active()
                .taskUnassigned()
                .list();
        if (unassigned == null || unassigned.isEmpty()) {
            return;
        }
        for (Task task : unassigned) {
            if (hasCandidateLinks(task.getId())) {
                continue;
            }
            String designed = resolveDesignedAssignee(task);
            if (userIdStr.equals(designed)) {
                try {
                    taskService.setAssignee(task.getId(), userIdStr);
                } catch (Exception ignored) {
                    // 自愈失败不影响待办列表
                }
            }
        }
    }

    private Task requireActiveTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在或已完成");
        }
        return task;
    }

    private static String formatDelegationState(org.flowable.task.api.Task task) {
        return task.getDelegationState() == null ? null : task.getDelegationState().name();
    }

    private TaskVO toVo(Task task) {
        String title = null;
        String businessKey = null;
        String defKey = null;
        try {
            Map<String, Object> vars = runtimeService.getVariables(task.getProcessInstanceId());
            if (vars != null && vars.get(WorkflowVars.TITLE) != null) {
                title = String.valueOf(vars.get(WorkflowVars.TITLE));
            }
            HistoricProcessInstance h = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (h != null) {
                businessKey = h.getBusinessKey();
                defKey = h.getProcessDefinitionKey();
            }
        } catch (Exception ignored) {
            // ignore variable load errors for list views
        }
        boolean canUnclaim = task.getAssignee() != null
                && !taskAddSignSupport.isAddSignPending(task.getId())
                && (task.getDelegationState() == null
                || !"PENDING".equals(task.getDelegationState().name()))
                && hasCandidateLinks(task.getId());
        return TaskVO.builder()
                .id(task.getId())
                .name(task.getName())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processDefinitionKey(defKey)
                .businessKey(businessKey)
                .assignee(task.getAssignee())
                .assigneeName(workflowIdentityFacade.resolveDisplayName(task.getAssignee()))
                .owner(task.getOwner())
                .delegationState(formatDelegationState(task))
                .addSignMode(localString(task.getId(), WorkflowVars.ADD_SIGN_MODE))
                .multiInstance(isMultiInstanceTask(task))
                .canUnclaim(canUnclaim)
                .formKey(task.getFormKey())
                .createTime(task.getCreateTime())
                .title(title)
                .build();
    }

    private TaskVO toVo(HistoricTaskInstance task) {
        HistoricProcessInstance h = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .includeProcessVariables()
                .singleResult();
        String title = null;
        String businessKey = null;
        String defKey = null;
        if (h != null) {
            businessKey = h.getBusinessKey();
            defKey = h.getProcessDefinitionKey();
            if (h.getProcessVariables() != null && h.getProcessVariables().get(WorkflowVars.TITLE) != null) {
                title = String.valueOf(h.getProcessVariables().get(WorkflowVars.TITLE));
            }
        }
        return TaskVO.builder()
                .id(task.getId())
                .name(task.getName())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processDefinitionKey(defKey)
                .businessKey(businessKey)
                .assignee(task.getAssignee())
                .assigneeName(workflowIdentityFacade.resolveDisplayName(task.getAssignee()))
                .formKey(task.getFormKey())
                .createTime(task.getCreateTime())
                .endTime(task.getEndTime())
                .title(title)
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
