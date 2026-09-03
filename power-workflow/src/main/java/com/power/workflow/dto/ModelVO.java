package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模型草稿视图。
 */
@Data
@Builder
@Schema(description = "流程模型草稿视图")
public class ModelVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "模型 key")
    private String modelKey;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "BPMN XML")
    private String bpmnXml;

    @Schema(description = "草稿版本")
    private Integer version;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
