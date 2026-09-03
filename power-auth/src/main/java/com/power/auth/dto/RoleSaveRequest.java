package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "角色保存请求")
public class RoleSaveRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "角色编码")
    private String roleCode;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "排序，默认 0")
    private Integer sort;

    @Schema(description = "状态：0停用 1正常，默认 1")
    private Integer status;

    @Size(max = 255)
    @Schema(description = "备注")
    private String remark;
}
