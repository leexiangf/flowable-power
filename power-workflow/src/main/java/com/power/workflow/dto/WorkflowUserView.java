package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Feign 反序列化用的用户视图（与 auth WorkflowUserVO 字段对齐）。
 */
@Data
@Schema(description = "工作流用户简要信息")
public class WorkflowUserView {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private Integer status;
}
