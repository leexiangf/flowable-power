package com.power.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.middleware.mq.outbox.OutboxService;
import com.power.middleware.security.Authz;
import com.power.middleware.security.SecurityUtils;
import com.power.workflow.constant.WorkflowMqTopics;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.domain.WfUrgeLog;
import com.power.workflow.dto.ProcessUrgeRequest;
import com.power.workflow.mapper.WfUrgeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程催办：写日志 + Outbox 事件（首版不接真实通知通道）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceUrgeAppService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final WfUrgeLogMapper urgeLogMapper;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final Authz authz;

    @Transactional(rollbackFor = Exception.class)
    public void urge(String processInstanceId, ProcessUrgeRequest request) {
        Long userId = requireLoginUserId();
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (runtime == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "流程已结束或不存在，无法催办");
        }
        assertCanUrge(processInstanceId, userId, runtime.getStartUserId());

        Long toUserId = null;
        if (request != null && StringUtils.hasText(request.getTargetUserId())) {
            try {
                toUserId = Long.valueOf(request.getTargetUserId().trim());
            } catch (NumberFormatException ex) {
                throw new BizException(ErrorCode.BAD_REQUEST, "targetUserId 格式无效");
            }
        }

        String comment = request != null && StringUtils.hasText(request.getComment())
                ? request.getComment().trim()
                : "请尽快处理";

        WfUrgeLog logEntity = new WfUrgeLog();
        logEntity.setProcessInstanceId(processInstanceId);
        logEntity.setFromUserId(userId);
        logEntity.setToUserId(toUserId);
        logEntity.setComment(comment);
        urgeLogMapper.insert(logEntity);

        List<String> pendingAssignees = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list()
                .stream()
                .map(Task::getAssignee)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", WorkflowMqTopics.TAG_PROCESS_URGED);
        payload.put("processInstanceId", processInstanceId);
        payload.put("fromUserId", String.valueOf(userId));
        payload.put("fromUsername", SecurityUtils.currentUsername());
        payload.put("toUserId", toUserId == null ? null : String.valueOf(toUserId));
        payload.put("comment", comment);
        payload.put("pendingAssignees", pendingAssignees);
        if (historic != null) {
            payload.put("processDefinitionKey", historic.getProcessDefinitionKey());
            payload.put("businessKey", historic.getBusinessKey());
            payload.put("startUserId", historic.getStartUserId());
            Map<String, Object> vars = historic.getProcessVariables();
            if (vars != null) {
                payload.put("title", vars.get(WorkflowVars.TITLE));
            }
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxService.enqueue(WorkflowMqTopics.EXCHANGE, WorkflowMqTopics.TAG_PROCESS_URGED, json);
        } catch (Exception ex) {
            log.error("Failed to enqueue urge outbox for pi={}", processInstanceId, ex);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "催办事件写入失败");
        }
    }

    /**
     * Controller 已校验 {@code workflow:task:urge}；此处再限制：仅发起人或监控员可催办任意实例。
     */
    private void assertCanUrge(String processInstanceId, Long userId, String startUserId) {
        if (Objects.equals(String.valueOf(userId), startUserId)) {
            return;
        }
        if (authz.permit("workflow:instance:monitor")) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "仅发起人或监控员可催办该流程");
    }

    private Long requireLoginUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
