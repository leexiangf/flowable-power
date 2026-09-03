package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 工作流用户视图（不含密码）。
 * <p>
 * Flowable {@code assignee} 使用 {@code String.valueOf(userId)}。
 */
@Data
@Builder
@Schema(description = "工作流用户视图")
public class WorkflowUserVO {

    /** 用户 ID（对应 assignee） */
    @Schema(description = "用户 ID")
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 昵称（展示优先） */
    @Schema(description = "昵称")
    private String nickname;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 状态：0停用 1正常 */
    @Schema(description = "状态：0停用 1正常")
    private Integer status;
}
