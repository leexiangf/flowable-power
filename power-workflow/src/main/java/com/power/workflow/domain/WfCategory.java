package com.power.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程分类实体，对应表 {@code wf_category}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_category")
public class WfCategory extends BaseEntity {

    /** 分类编码（部署时可写入 Flowable category） */
    private String code;

    /** 分类名称 */
    private String name;

    /** 排序（升序） */
    private Integer sort;

    /** 状态：0停用 1正常 */
    private Integer status;

    /** 备注 */
    private String remark;
}
