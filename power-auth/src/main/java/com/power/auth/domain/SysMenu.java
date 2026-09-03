package com.power.auth.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单 / 目录 / 按钮（权限点）。
 * <p>
 * menu_type：1 目录 · 2 菜单 · 3 按钮
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
    private String menuName;
    /** 1目录 2菜单 3按钮 */
    private Integer menuType;
    /** 前端路由 path */
    private String path;
    /** 前端组件路径 */
    private String component;
    /** 权限码 */
    private String perms;
    private String icon;
    private Integer sort;
    /** 0隐藏 1显示 */
    private Integer visible;
    /** 0停用 1正常 */
    private Integer status;
    private String remark;
}
