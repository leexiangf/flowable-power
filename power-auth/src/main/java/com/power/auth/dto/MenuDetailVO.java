package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Schema(description = "菜单详情（管理端）")
public class MenuDetailVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private String remark;
    @Schema(description = "是否内置菜单（只读，不可修改删除）")
    private Boolean builtIn;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Builder.Default
    private List<MenuDetailVO> children = new ArrayList<>();
}
