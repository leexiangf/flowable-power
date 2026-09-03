package com.power.workflow.dto.leave;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请视图。
 */
@Data
@Builder
@Schema(description = "请假申请视图")
public class LeaveVO {

    /** 请假单主键 */
    @Schema(description = "请假单 ID")
    private Long id;

    /** 申请人用户 ID */
    @Schema(description = "申请人用户 ID")
    private Long userId;

    /** 申请人登录名 */
    @Schema(description = "申请人登录名")
    private String username;

    /** 请假天数 */
    @Schema(description = "请假天数")
    private BigDecimal days;

    /** 请假事由 */
    @Schema(description = "请假事由")
    private String reason;

    /** 开始日期 */
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期 */
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /**
     * 状态：1审批中 2通过 3驳回 4撤销。
     */
    @Schema(description = "状态：1审批中 2通过 3驳回 4撤销")
    private Integer status;

    /** 关联流程实例 ID */
    @Schema(description = "流程实例 ID")
    private String processInstanceId;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
