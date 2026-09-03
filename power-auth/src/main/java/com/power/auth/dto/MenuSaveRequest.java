package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "菜单保存请求")
public class MenuSaveRequest {

    @NotNull
    @Schema(description = "父节点 ID，根为 0")
    private Long parentId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "名称")
    private String menuName;

    @NotNull
    @Schema(description = "类型：1目录 2菜单 3按钮")
    private Integer menuType;

    @Size(max = 255)
    @Schema(description = "路由 path")
    private String path;

    @Size(max = 255)
    @Schema(description = "组件路径")
    private String component;

    @Size(max = 128)
    @Schema(description = "权限码")
    private String perms;

    @Size(max = 64)
    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序，默认 0")
    private Integer sort;

    @Schema(description = "是否显示：0隐藏 1显示，默认 1")
    private Integer visible;

    @Schema(description = "状态：0停用 1正常，默认 1")
    private Integer status;

    @Size(max = 255)
    @Schema(description = "备注")
    private String remark;
}
