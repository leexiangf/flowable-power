package com.power.workflow.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 费用报销发起请求（变量办理人 + 会签）。
 */
@Data
@Schema(description = "费用报销发起请求")
public class ExpenseStartRequest {

    @NotBlank
    @Schema(description = "部门经理用户 ID（变量办理人）", example = "1")
    private String managerUserId;

    @NotEmpty
    @Size(min = 1, max = 20)
    @Schema(description = "会签人用户 ID 列表", example = "[\"3\",\"1\"]")
    private List<String> countersignUserIds;

    @Schema(description = "标题", example = "差旅报销-上海")
    private String title;

    @Schema(description = "金额说明（流程变量）", example = "1200.50")
    private String amount;

    @Schema(description = "事由", example = "客户拜访差旅")
    private String reason;
}
