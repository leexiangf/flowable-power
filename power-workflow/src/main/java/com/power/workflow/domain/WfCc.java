package com.power.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程抄送记录，对应表 {@code wf_cc}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_cc")
public class WfCc extends BaseEntity {

    /** 流程实例 ID */
    private String processInstanceId;

    /** 关联任务 ID（可空） */
    private String taskId;

    /** 抄送人用户 ID */
    private Long userId;

    /** 抄送操作人用户 ID */
    private Long fromUserId;

    /** 0未读 1已读 */
    private Integer readFlag;
}
