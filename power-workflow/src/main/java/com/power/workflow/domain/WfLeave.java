package com.power.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 请假申请实体，对应表 {@code wf_leave}。
 * <p>
 * 流程 businessKey = 本表主键 id。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_leave")
public class WfLeave extends BaseEntity {

    /** 申请人用户 ID */
    private Long userId;

    /** 申请人登录名 */
    private String username;

    /** 请假天数 */
    private BigDecimal days;

    /** 请假事由 */
    private String reason;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /**
     * 状态：1审批中 2通过 3驳回 4撤销。
     */
    private Integer status;

    /** 关联流程实例 ID */
    private String processInstanceId;
}
