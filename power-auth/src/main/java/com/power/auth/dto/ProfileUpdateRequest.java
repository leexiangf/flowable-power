package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "个人资料更新")
public class ProfileUpdateRequest {

    @Size(max = 64)
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 128)
    @Schema(description = "邮箱")
    private String email;

    @Size(max = 20)
    @Schema(description = "手机号")
    private String phone;

    @Size(max = 255)
    @Schema(description = "头像 URL")
    private String avatar;
}
