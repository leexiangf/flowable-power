package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理员强制终止流程请求。
 */
@Data
@Schema(description = "强制终止流程请求")
public class ProcessTerminateRequest {

    @Schema(description = "终止原因", example = "管理员强制终止")
    private String reason;
}
