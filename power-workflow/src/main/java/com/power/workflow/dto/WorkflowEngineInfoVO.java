package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Flowable Process 引擎探活信息。
 */
@Data
@Builder
@Schema(description = "引擎探活信息")
public class WorkflowEngineInfoVO {

    /** 引擎名称 */
    @Schema(description = "引擎名称")
    private String engineName;

    /** Flowable 版本 */
    @Schema(description = "引擎版本")
    private String version;

    /** 异步作业执行器是否启用 */
    @Schema(description = "异步执行器是否启用")
    private boolean asyncExecutorActive;
}
