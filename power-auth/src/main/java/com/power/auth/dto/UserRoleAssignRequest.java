package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户角色分配")
public class UserRoleAssignRequest {

    @NotNull
    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;
}
