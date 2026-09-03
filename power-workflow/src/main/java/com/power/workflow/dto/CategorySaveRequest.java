package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程分类保存请求。
 */
@Data
@Schema(description = "流程分类保存请求")
public class CategorySaveRequest {

    @NotBlank
    @Schema(description = "分类编码", example = "leave")
    private String code;

    @NotBlank
    @Schema(description = "分类名称", example = "请假")
    private String name;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态：0停用 1正常", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
