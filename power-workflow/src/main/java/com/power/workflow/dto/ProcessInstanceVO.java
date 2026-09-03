package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 流程实例视图。
 */
@Data
@Builder
@Schema(description = "流程实例视图")
public class ProcessInstanceVO {

    /** 流程实例 ID */
    @Schema(description = "流程实例 ID")
    private String id;

    /** 流程定义 ID */
    @Schema(description = "流程定义 ID")
    private String processDefinitionId;

    /** 流程定义 key */
    @Schema(description = "流程定义 key")
    private String processDefinitionKey;

    /** 流程定义名称 */
    @Schema(description = "流程定义名称")
    private String processDefinitionName;

    /** 业务主键（如请假单 id） */
    @Schema(description = "业务主键")
    private String businessKey;

    /** 发起人用户 ID（字符串） */
    @Schema(description = "发起人用户 ID")
    private String startUserId;

    /** 发起人展示名（昵称优先） */
    @Schema(description = "发起人展示名")
    private String startUserName;

    /** 启动时间 */
    @Schema(description = "启动时间")
    private Date startTime;

    /** 结束时间；未结束则为 null */
    @Schema(description = "结束时间")
    private Date endTime;

    /** 是否已结束 */
    @Schema(description = "是否已结束")
    private boolean ended;

    /** 运行中实例是否挂起 */
    @Schema(description = "是否挂起")
    private boolean suspended;

    /** 流程标题（来自变量 title） */
    @Schema(description = "流程标题")
    private String title;

    /** 流程变量快照 */
    @Schema(description = "流程变量")
    private Map<String, Object> variables;
}
