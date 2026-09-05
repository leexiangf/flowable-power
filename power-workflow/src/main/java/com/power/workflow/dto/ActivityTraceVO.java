package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 流程流转节点（时间线单项）。
 */
@Data
@Builder
@Schema(description = "流程流转节点")
public class ActivityTraceVO {

    /** 活动节点 ID（BPMN element id） */
    @Schema(description = "活动节点 ID")
    private String activityId;

    /** 活动名称 */
    @Schema(description = "活动名称")
    private String activityName;

    /** 活动类型，如 userTask、startEvent、endEvent */
    @Schema(description = "活动类型")
    private String activityType;

    /** 办理人（用户任务，userId 字符串） */
    @Schema(description = "办理人用户 ID")
    private String assignee;

    /** 办理人展示名（昵称优先） */
    @Schema(description = "办理人展示名")
    private String assigneeName;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private Date startTime;

    /** 结束时间；进行中为空 */
    @Schema(description = "结束时间")
    private Date endTime;

    /** 耗时（毫秒） */
    @Schema(description = "耗时毫秒")
    private Long durationInMillis;

    /** 审批意见（来自任务 comment） */
    @Schema(description = "审批意见")
    private String comment;
}
