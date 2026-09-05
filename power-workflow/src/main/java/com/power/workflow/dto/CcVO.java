package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抄送列表视图。
 */
@Data
@Builder
@Schema(description = "抄送记录")
public class CcVO {

    private String id;

    private String processInstanceId;

    private String taskId;

    private String processDefinitionKey;

    private String title;

    private String businessKey;

    private Long fromUserId;

    private String fromUserName;

    private Integer readFlag;

    private LocalDateTime createTime;

    private Boolean ended;
}
