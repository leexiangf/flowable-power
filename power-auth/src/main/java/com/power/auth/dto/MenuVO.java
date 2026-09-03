package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端路由 / 侧边栏菜单树节点。
 */
@Data
@Builder
@Schema(description = "菜单树节点")
public class MenuVO {

    @Schema(description = "菜单 ID")
    private Long id;

    @Schema(description = "父节点 ID，根为 0")
    private Long parentId;

    @Schema(description = "名称")
    private String menuName;

    @Schema(description = "类型：1目录 2菜单 3按钮")
    private Integer menuType;

    @Schema(description = "路由 path")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限码（按钮级）")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子节点")
    @Builder.Default
    private List<MenuVO> children = new ArrayList<>();
}
