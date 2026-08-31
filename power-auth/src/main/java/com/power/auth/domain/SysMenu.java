package com.power.auth.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.power.middleware.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
    private String menuName;
    private String perms;
    private Integer menuType;
    private Integer sort;
}
