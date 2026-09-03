package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 转办任务请求。
 */
@Data
@Schema(description = "转办任务请求")
public class TaskTransferRequest {

    @NotBlank
    @Schema(description = "目标办理人用户 ID", example = "3")
    private String targetUserId;

    @Schema(description = "转办说明", example = "请代为审批")
    private String comment;
}
