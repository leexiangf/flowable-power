package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.workflow.dto.ActivityTraceVO;
import com.power.workflow.dto.ProcessHighlightVO;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程追踪：流转时间线、高亮数据、流程图 PNG。
 */
@Service
@RequiredArgsConstructor
public class ProcessTraceAppService {

    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final TaskService taskService;
    private final ProcessDefinitionAppService processDefinitionAppService;
    private final ProcessEngineConfiguration processEngineConfiguration;
    private final ProcessInstanceAppService processInstanceAppService;
    private final WorkflowIdentityFacade workflowIdentityFacade;

    /**
     * 查询实例流转时间线（含审批意见）。
     *
     * @param processInstanceId 流程实例 ID
     * @return 活动节点列表
     */
    public List<ActivityTraceVO> timeline(String processInstanceId) {
        processInstanceAppService.assertCanViewInstance(processInstanceId);
        requireHistoric(processInstanceId);
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        Map<String, String> commentByTaskId = taskService.getProcessInstanceComments(processInstanceId).stream()
                .filter(c -> c.getTaskId() != null)
                .collect(Collectors.toMap(Comment::getTaskId, Comment::getFullMessage, (a, b) -> a + " | " + b));

        List<ActivityTraceVO> result = new ArrayList<>();
        for (HistoricActivityInstance act : activities) {
            if ("sequenceFlow".equals(act.getActivityType())) {
                continue;
            }
            String assignee = act.getAssignee();
            result.add(ActivityTraceVO.builder()
                    .activityId(act.getActivityId())
                    .activityName(act.getActivityName())
                    .activityType(act.getActivityType())
                    .assignee(assignee)
                    .assigneeName(workflowIdentityFacade.resolveDisplayName(assignee))
                    .startTime(act.getStartTime())
                    .endTime(act.getEndTime())
                    .durationInMillis(act.getDurationInMillis())
                    .comment(act.getTaskId() == null ? null : commentByTaskId.get(act.getTaskId()))
                    .build());
        }
        result.sort(Comparator.comparing(ActivityTraceVO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * 返回流程图高亮所需数据（当前/已完成节点 + BPMN XML）。
     *
     * @param processInstanceId 流程实例 ID
     * @return 高亮视图
     */
    public ProcessHighlightVO highlight(String processInstanceId) {
        processInstanceAppService.assertCanViewInstance(processInstanceId);
        HistoricProcessInstance historic = requireHistoric(processInstanceId);
        Set<String> finished = new LinkedHashSet<>();
        historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list()
                .forEach(a -> {
                    if (!"sequenceFlow".equals(a.getActivityType())) {
                        finished.add(a.getActivityId());
                    }
                });

        List<String> active = new ArrayList<>();
        if (historic.getEndTime() == null) {
            active.addAll(runtimeService.getActiveActivityIds(processInstanceId));
        }

        String xml = processDefinitionAppService.getBpmnXml(historic.getProcessDefinitionId());
        return ProcessHighlightVO.builder()
                .processInstanceId(processInstanceId)
                .processDefinitionId(historic.getProcessDefinitionId())
                .activeActivityIds(active)
                .finishedActivityIds(new ArrayList<>(finished))
                .bpmnXml(xml)
                .build();
    }

    /**
     * 生成带高亮的流程图 PNG。
     *
     * @param processInstanceId 流程实例 ID
     * @return PNG 字节
     */
    public byte[] diagramPng(String processInstanceId) {
        processInstanceAppService.assertCanViewInstance(processInstanceId);
        HistoricProcessInstance historic = requireHistoric(processInstanceId);
        List<String> highLightedActivities = new ArrayList<>();
        if (historic.getEndTime() == null) {
            highLightedActivities.addAll(runtimeService.getActiveActivityIds(processInstanceId));
        } else {
            historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .activityType("endEvent")
                    .finished()
                    .list()
                    .forEach(a -> highLightedActivities.add(a.getActivityId()));
        }
        List<String> highLightedFlows = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("sequenceFlow")
                .list()
                .stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());

        BpmnModel bpmnModel = repositoryService.getBpmnModel(historic.getProcessDefinitionId());
        ProcessDiagramGenerator generator = processEngineConfiguration.getProcessDiagramGenerator();
        try (InputStream diagram = generator.generateDiagram(
                bpmnModel,
                "png",
                highLightedActivities,
                highLightedFlows,
                processEngineConfiguration.getActivityFontName(),
                processEngineConfiguration.getLabelFontName(),
                processEngineConfiguration.getAnnotationFontName(),
                processEngineConfiguration.getClassLoader(),
                1.0,
                true)) {
            return diagram.readAllBytes();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "生成流程图失败: " + ex.getMessage());
        }
    }

    private HistoricProcessInstance requireHistoric(String processInstanceId) {
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historic == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程实例不存在");
        }
        return historic;
    }
}
