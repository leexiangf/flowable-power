package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 驳回任务请求。
 */
@Data
@Schema(description = "驳回任务请求")
public class TaskRejectRequest {

    @Schema(description = "驳回意见", example = "请补充材料")
    private String comment;

    /**
     * 驳回策略：
     * <ul>
     *   <li>PREVIOUS — 退回上一用户任务（默认，targetActivityId 可覆盖）</li>
     *   <li>TO_NODE — 退回指定 activityId（须传 targetActivityId）</li>
     *   <li>TO_STARTER — 退回发起后的第一个用户任务</li>
     *   <li>TERMINATE — 驳回并结束流程（approved=false）</li>
     * </ul>
     */
    @Schema(description = "驳回策略：PREVIOUS / TO_NODE / TO_STARTER / TERMINATE", example = "PREVIOUS")
    private String strategy;

    @Schema(description = "目标活动节点 ID；TO_NODE 必填，PREVIOUS 时可覆盖自动探测")
    private String targetActivityId;
}
