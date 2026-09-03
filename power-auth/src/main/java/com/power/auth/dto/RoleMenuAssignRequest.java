package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色菜单分配")
public class RoleMenuAssignRequest {

    @NotNull
    @Schema(description = "菜单 ID 列表")
    private List<Long> menuIds;
}
