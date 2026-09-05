package com.power.workflow.support;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.dto.TaskAddSignRequest;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 加签能力：前加签 / 后加签（指定一人），不改 BPMN。
 * <p>
 * 前加签：assignee→加签人，办完归还原办理人（拦截 complete，不推进节点）。<br>
 * 后加签：记录本人意见后 assignee→加签人，加签人 complete 推进流程。
 */
@Component
@RequiredArgsConstructor
public class TaskAddSignSupport {

    public static final String TYPE_BEFORE = "BEFORE";
    public static final String TYPE_AFTER = "AFTER";

    private final TaskService taskService;

    /**
     * 执行加签。
     */
    public void addSign(Task task, Long operatorUserId, TaskAddSignRequest request) {
        if (request == null || !StringUtils.hasText(request.getTargetUserId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 不能为空");
        }
        String type = request.getType() == null ? "" : request.getType().trim().toUpperCase();
        if (!TYPE_BEFORE.equals(type) && !TYPE_AFTER.equals(type)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "type 仅支持 BEFORE / AFTER");
        }
        String targetUserId = request.getTargetUserId().trim();
        String operator = String.valueOf(operatorUserId);
        if (Objects.equals(operator, targetUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能加签给自己");
        }
        if (task.getDelegationState() != null && "PENDING".equals(task.getDelegationState().name())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "委派中的任务请先归还再加签");
        }
        if (isAddSignPending(task.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "当前任务已在加签中");
        }
        if (task.getAssignee() == null) {
            taskService.claim(task.getId(), operator);
        }

        if (StringUtils.hasText(request.getComment())) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), request.getComment().trim());
        }

        taskService.setVariableLocal(task.getId(), WorkflowVars.ADD_SIGN_MODE, type);
        taskService.setVariableLocal(task.getId(), WorkflowVars.ADD_SIGN_RETURN_TO, operator);
        taskService.setOwner(task.getId(), operator);
        taskService.setAssignee(task.getId(), targetUserId);
    }

    /**
     * 前加签拦截：加签人「完成」时归还原办理人，不推进流程。返回 true 表示已处理。
     */
    public boolean tryResolveBeforeAddSign(Task task, Long operatorUserId, String comment) {
        Object mode = taskService.getVariableLocal(task.getId(), WorkflowVars.ADD_SIGN_MODE);
        if (mode == null || !TYPE_BEFORE.equals(String.valueOf(mode))) {
            return false;
        }
        String returnTo = stringVar(taskService.getVariableLocal(task.getId(), WorkflowVars.ADD_SIGN_RETURN_TO));
        if (!StringUtils.hasText(returnTo)) {
            returnTo = task.getOwner();
        }
        if (!Objects.equals(String.valueOf(operatorUserId), task.getAssignee())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅当前加签办理人可归还");
        }
        if (StringUtils.hasText(comment)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), comment.trim());
        } else {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), "前加签办结，归还原办理人");
        }
        clearAddSignLocals(task.getId());
        taskService.setAssignee(task.getId(), returnTo);
        return true;
    }

    public boolean isAddSignPending(String taskId) {
        Object mode = taskService.getVariableLocal(taskId, WorkflowVars.ADD_SIGN_MODE);
        return mode != null && StringUtils.hasText(String.valueOf(mode));
    }

    public void clearAddSignLocals(String taskId) {
        taskService.removeVariableLocal(taskId, WorkflowVars.ADD_SIGN_MODE);
        taskService.removeVariableLocal(taskId, WorkflowVars.ADD_SIGN_RETURN_TO);
    }

    private static String stringVar(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
