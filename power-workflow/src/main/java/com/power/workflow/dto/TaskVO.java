package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 用户任务视图（待办 / 已办）。
 */
@Data
@Builder
@Schema(description = "用户任务视图")
public class TaskVO {

    /** 任务 ID */
    @Schema(description = "任务 ID")
    private String id;

    /** 任务名称 */
    @Schema(description = "任务名称")
    private String name;

    /** 所属流程实例 ID */
    @Schema(description = "流程实例 ID")
    private String processInstanceId;

    /** 流程定义 ID */
    @Schema(description = "流程定义 ID")
    private String processDefinitionId;

    /** 流程定义 key */
    @Schema(description = "流程定义 key")
    private String processDefinitionKey;

    /** 业务主键 */
    @Schema(description = "业务主键")
    private String businessKey;

    /** 办理人用户 ID；未认领时为空 */
    @Schema(description = "办理人用户 ID")
    private String assignee;

    /** 办理人展示名（昵称优先） */
    @Schema(description = "办理人展示名")
    private String assigneeName;

    /** 外置表单标识（formKey） */
    @Schema(description = "表单 formKey")
    private String formKey;

    /** 任务创建时间 */
    @Schema(description = "创建时间")
    private Date createTime;

    /** 任务完成时间；待办为空 */
    @Schema(description = "完成时间")
    private Date endTime;

    /** 流程标题 */
    @Schema(description = "流程标题")
    private String title;
}
