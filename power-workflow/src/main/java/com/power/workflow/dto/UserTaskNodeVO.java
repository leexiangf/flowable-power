package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 可驳回的用户任务节点。
 */
@Data
@Builder
@Schema(description = "用户任务节点")
public class UserTaskNodeVO {

    private String activityId;

    private String activityName;
}
