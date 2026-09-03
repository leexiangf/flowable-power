package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户保存请求")
public class UserSaveRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "登录名")
    private String username;

    @Size(min = 6, max = 64)
    @Schema(description = "密码（新增必填；修改时留空表示不改）")
    private String password;

    @Size(max = 64)
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 20)
    @Schema(description = "手机号")
    private String phone;

    @Size(max = 128)
    @Schema(description = "邮箱")
    private String email;

    @Size(max = 255)
    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "状态：0停用 1正常，默认 1")
    private Integer status;

    @Size(max = 255)
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;
}
