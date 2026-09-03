package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 完成任务请求。
 */
@Data
@Schema(description = "完成任务请求")
public class TaskCompleteRequest {

    /** 审批意见 */
    @Schema(description = "审批意见", example = "同意")
    private String comment;

    /** 任务/流程变量；默认会写入 approved=true */
    @Schema(description = "任务/流程变量；默认写入 approved=true")
    private Map<String, Object> variables = new HashMap<>();
}
