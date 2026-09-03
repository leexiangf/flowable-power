package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.SecurityUtils;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.TaskCompleteRequest;
import com.power.workflow.dto.TaskRejectRequest;
import com.power.workflow.dto.TaskTransferRequest;
import com.power.workflow.dto.TaskVO;
import com.power.workflow.support.FlowableUserContext;
import com.power.workflow.support.WorkflowApprovals;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
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
    private final WorkflowIdentityFacade workflowIdentityFacade;

    /**
     * 当前用户待办（办理人 + 候选人/候选人组）。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 待办分页
     */
    public PageResult<TaskVO> listTodo(long pageNum, long pageSize) {
        Long userId = requireLoginUserId();
        String userIdStr = String.valueOf(userId);
        List<String> roleCodes = workflowIdentityFacade.listRoleCodes(userId);

        TaskQuery query = taskService.createTaskQuery().active().orderByTaskCreateTime().desc();
        if (roleCodes == null || roleCodes.isEmpty()) {
            query.or().taskAssignee(userIdStr).taskCandidateUser(userIdStr).endOr();
        } else {
            query.or()
                    .taskAssignee(userIdStr)
                    .taskCandidateUser(userIdStr)
                    .taskCandidateGroupIn(roleCodes)
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
     * 取消本人已认领的任务。
     *
     * @param taskId 任务 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unclaim(String taskId) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        if (!Objects.equals(String.valueOf(userId), task.getAssignee())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能取消自己认领的任务");
        }
        FlowableUserContext.runAs(userId, () -> taskService.unclaim(taskId));
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

        Map<String, Object> vars = new HashMap<>();
        if (request != null && request.getVariables() != null) {
            vars.putAll(request.getVariables());
        }
        // 缺省通过；若客户端传入字符串/数字则规范为 Boolean，避免网关条件失败
        boolean approved = WorkflowApprovals.toBoolean(vars.get(WorkflowVars.APPROVED), true);
        // 会签：任一实例已写入 false 后不可被后续 complete 覆盖为 true
        if (isMultiInstanceTask(task)) {
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
            if (request != null && StringUtils.hasText(request.getComment())) {
                taskService.addComment(taskId, task.getProcessInstanceId(), request.getComment().trim());
            }
            taskService.complete(taskId, vars);
        });
    }

    /**
     * 驳回：
     * <ul>
     *   <li>多实例（会签）节点：以 {@code approved=false} 完成当前实例，由后续网关决定走向</li>
     *   <li>存在上一用户任务且非多实例：退回上一节点</li>
     *   <li>否则：以 {@code approved=false} 完成（如请假单任务直接结束）</li>
     * </ul>
     *
     * @param taskId  任务 ID
     * @param request 驳回入参
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, TaskRejectRequest request) {
        Long userId = requireLoginUserId();
        Task task = requireActiveTask(taskId);
        assertCanComplete(task, userId);
        String comment = request != null && StringUtils.hasText(request.getComment())
                ? request.getComment().trim()
                : "驳回";
        boolean multiInstance = isMultiInstanceTask(task);
        String targetActivityId = null;
        if (!multiInstance) {
            targetActivityId = request != null ? request.getTargetActivityId() : null;
            if (!StringUtils.hasText(targetActivityId)) {
                targetActivityId = findPreviousUserTaskActivityId(task);
            }
        }

        String finalTarget = targetActivityId;
        FlowableUserContext.runAs(userId, () -> {
            if (task.getAssignee() == null) {
                taskService.claim(taskId, String.valueOf(userId));
            }
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            runtimeService.setVariable(task.getProcessInstanceId(), WorkflowVars.APPROVED, false);
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
        if (request == null || !StringUtils.hasText(request.getTargetUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 不能为空");
        }
        String targetUserId = request.getTargetUserId().trim();
        if (Objects.equals(String.valueOf(userId), targetUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能转办给自己");
        }
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

    private Task requireActiveTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在或已完成");
        }
        return task;
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
