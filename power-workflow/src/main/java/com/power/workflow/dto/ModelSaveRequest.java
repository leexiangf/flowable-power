package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程模型草稿保存请求（bpmn-js 保存）。
 */
@Data
@Schema(description = "流程模型保存请求")
public class ModelSaveRequest {

    @NotBlank
    @Schema(description = "模型 key", example = "leave")
    private String modelKey;

    @NotBlank
    @Schema(description = "模型名称", example = "请假审批")
    private String name;

    @Schema(description = "分类编码", example = "leave")
    private String categoryCode;

    @NotBlank
    @Schema(description = "BPMN XML 内容")
    private String bpmnXml;

    @Schema(description = "备注")
    private String remark;
}
