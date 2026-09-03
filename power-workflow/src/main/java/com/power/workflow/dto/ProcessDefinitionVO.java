package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 流程定义视图。
 */
@Data
@Builder
@Schema(description = "流程定义视图")
public class ProcessDefinitionVO {

    /** 流程定义 ID（含版本） */
    @Schema(description = "流程定义 ID")
    private String id;

    /** 流程定义 key，如 leave */
    @Schema(description = "流程定义 key")
    private String key;

    /** 流程名称 */
    @Schema(description = "流程名称")
    private String name;

    /** 版本号（同 key 递增） */
    @Schema(description = "版本号")
    private int version;

    /** 部署 ID */
    @Schema(description = "部署 ID")
    private String deploymentId;

    /** 分类 */
    @Schema(description = "分类")
    private String category;

    /** 是否已挂起（挂起后不可新启实例） */
    @Schema(description = "是否已挂起")
    private boolean suspended;

    /** 部署时间 */
    @Schema(description = "部署时间")
    private Date deploymentTime;
}
