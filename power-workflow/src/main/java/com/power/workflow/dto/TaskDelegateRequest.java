package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 任务委派请求。
 */
@Data
@Schema(description = "任务委派请求")
public class TaskDelegateRequest {

    @NotBlank
    @Schema(description = "委派目标用户 ID（字符串）", example = "3")
    private String targetUserId;

    @Schema(description = "委派说明")
    private String comment;

    @Schema(description = "抄送人 userId 列表")
    private List<String> ccUserIds;
}
