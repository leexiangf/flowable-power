package com.power.workflow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power.middleware.mq.outbox.OutboxService;
import com.power.workflow.constant.WorkflowMqTopics;
import com.power.workflow.constant.WorkflowVars;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程结束 / 撤销时写入 Outbox，由定时任务投递到 RabbitMQ {@code power.workflow}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessLifecycleOutboxListener implements FlowableEventListener {

    private final OutboxService outboxService;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper;

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEntityEvent entityEvent)) {
            return;
        }
        if (event.getType() == FlowableEngineEventType.PROCESS_COMPLETED) {
            enqueue(entityEvent, WorkflowMqTopics.TAG_PROCESS_COMPLETED);
        } else if (event.getType() == FlowableEngineEventType.PROCESS_CANCELLED) {
            enqueue(entityEvent, WorkflowMqTopics.TAG_PROCESS_CANCELLED);
        }
    }

    private void enqueue(FlowableEngineEntityEvent entityEvent, String tag) {
        Object entity = entityEvent.getEntity();
        String processInstanceId = null;
        if (entity instanceof ExecutionEntity execution) {
            processInstanceId = execution.getProcessInstanceId();
            if (!StringUtils.hasText(processInstanceId)) {
                processInstanceId = execution.getId();
            }
        }
        if (!StringUtils.hasText(processInstanceId)) {
            log.warn("Skip outbox: cannot resolve processInstanceId for {}", tag);
            return;
        }

        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", tag);
        payload.put("processInstanceId", processInstanceId);
        if (historic != null) {
            payload.put("processDefinitionId", historic.getProcessDefinitionId());
            payload.put("processDefinitionKey", historic.getProcessDefinitionKey());
            payload.put("businessKey", historic.getBusinessKey());
            payload.put("startUserId", historic.getStartUserId());
            payload.put("startTime", historic.getStartTime() == null ? null : historic.getStartTime().getTime());
            payload.put("endTime", historic.getEndTime() == null ? null : historic.getEndTime().getTime());
            Map<String, Object> vars = historic.getProcessVariables();
            if (vars != null) {
                payload.put("businessType", vars.get(WorkflowVars.BUSINESS_TYPE));
                payload.put("title", vars.get(WorkflowVars.TITLE));
                payload.put("approved", vars.get(WorkflowVars.APPROVED));
            }
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxService.enqueue(WorkflowMqTopics.EXCHANGE, tag, json);
            log.info("Outbox enqueued {} for processInstanceId={}", tag, processInstanceId);
        } catch (Exception ex) {
            log.error("Failed to enqueue workflow outbox for pi={}", processInstanceId, ex);
            throw new IllegalStateException("写入流程 Outbox 失败", ex);
        }
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
