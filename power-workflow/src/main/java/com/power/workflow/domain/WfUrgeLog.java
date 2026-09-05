package com.power.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程催办记录，对应表 {@code wf_urge_log}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_urge_log")
public class WfUrgeLog extends BaseEntity {

    /** 流程实例 ID */
    private String processInstanceId;

    /** 催办人用户 ID */
    private Long fromUserId;

    /** 指定催办对象（可空） */
    private Long toUserId;

    /** 催办说明 */
    private String comment;
}
