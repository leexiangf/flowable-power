package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 驳回任务请求（退回上一用户任务节点）。
 */
@Data
@Schema(description = "驳回任务请求")
public class TaskRejectRequest {

    @Schema(description = "驳回意见", example = "请补充材料")
    private String comment;

    @Schema(description = "目标活动节点 ID；为空则自动退回上一用户任务")
    private String targetActivityId;
}
