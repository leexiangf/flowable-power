package com.power.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程模型草稿实体，对应表 {@code wf_model}（供 bpmn-js 保存）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_model")
public class WfModel extends BaseEntity {

    /** 模型 key，部署后作为 processDefinitionKey */
    private String modelKey;

    /** 模型名称 */
    private String name;

    /** 分类编码 */
    private String categoryCode;

    /** BPMN XML 内容 */
    private String bpmnXml;

    /** 草稿版本号 */
    private Integer version;

    /** 备注 */
    private String remark;
}
