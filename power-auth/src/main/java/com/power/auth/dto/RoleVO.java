package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "角色详情")
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<Long> menuIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
