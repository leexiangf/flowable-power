package com.power.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.power.auth.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 用户被授权的全部菜单节点（含目录/菜单/按钮）。
     */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
            INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND m.deleted = 0
              AND m.status = 1
            ORDER BY m.parent_id ASC, m.sort ASC, m.id ASC
            """)
    List<SysMenu> selectAssignedByUserId(@Param("userId") Long userId);
}
