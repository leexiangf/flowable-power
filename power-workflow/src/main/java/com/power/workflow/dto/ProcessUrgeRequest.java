package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程催办请求。
 */
@Data
@Schema(description = "流程催办请求")
public class ProcessUrgeRequest {

    @Schema(description = "催办说明", example = "请尽快处理")
    private String comment;

    @Schema(description = "指定催办对象 userId（可空=全体待办人）")
    private String targetUserId;
}
