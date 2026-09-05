package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 加签请求：前加签 / 后加签（指定一人）。
 */
@Data
@Schema(description = "加签请求")
public class TaskAddSignRequest {

    /** BEFORE=前加签（加签人办完归还本人）；AFTER=后加签（本人意见已记，交加签人推进） */
    @NotBlank
    @Schema(description = "加签类型：BEFORE / AFTER", example = "BEFORE")
    private String type;

    @NotBlank
    @Schema(description = "加签人 userId 字符串", example = "1")
    private String targetUserId;

    @Schema(description = "加签说明")
    private String comment;
}
