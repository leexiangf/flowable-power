package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 流程图高亮数据（供前端 bpmn-js 等渲染）。
 */
@Data
@Builder
@Schema(description = "流程图高亮数据")
public class ProcessHighlightVO {

    /** 流程实例 ID */
    @Schema(description = "流程实例 ID")
    private String processInstanceId;

    /** 流程定义 ID */
    @Schema(description = "流程定义 ID")
    private String processDefinitionId;

    /** 当前活动节点 ID 列表 */
    @Schema(description = "当前活动节点 ID")
    private List<String> activeActivityIds;

    /** 已完成活动节点 ID 列表 */
    @Schema(description = "已完成活动节点 ID")
    private List<String> finishedActivityIds;

    /** 对应流程定义的 BPMN XML */
    @Schema(description = "BPMN XML")
    private String bpmnXml;
}
