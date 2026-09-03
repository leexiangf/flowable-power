package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 撤销流程实例请求。
 */
@Data
@Schema(description = "撤销流程实例请求")
public class ProcessCancelRequest {

    @Schema(description = "撤销原因", example = "申请人撤销")
    private String reason;
}
