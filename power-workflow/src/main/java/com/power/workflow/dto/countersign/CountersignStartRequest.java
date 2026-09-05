package com.power.workflow.dto.countersign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 会签 / 或签发起请求。
 */
@Data
@Schema(description = "会签发起请求")
public class CountersignStartRequest {

    @NotEmpty
    @Size(min = 1, max = 20)
    @Schema(description = "会签 / 或签人用户 ID 列表", example = "[\"3\",\"1\"]")
    private List<String> countersignUserIds;

    @Schema(description = "标题", example = "并行或签审批")
    private String title;
}
