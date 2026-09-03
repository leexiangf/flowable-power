package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码")
public class PasswordChangeRequest {

    @NotBlank
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "新密码")
    private String newPassword;
}
