package com.power.workflow.dto.leave;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 请假申请创建请求。
 */
@Data
@Schema(description = "请假申请请求")
public class LeaveCreateRequest {

    /** 请假天数，最小 0.5 */
    @NotNull
    @DecimalMin("0.5")
    @Schema(description = "请假天数", example = "1.0")
    private BigDecimal days;

    /** 请假事由 */
    @NotBlank
    @Schema(description = "事由", example = "事假")
    private String reason;

    /** 开始日期 */
    @NotNull
    @Schema(description = "开始日期", example = "2026-09-02")
    private LocalDate startDate;

    /** 结束日期 */
    @NotNull
    @Schema(description = "结束日期", example = "2026-09-02")
    private LocalDate endDate;
}
