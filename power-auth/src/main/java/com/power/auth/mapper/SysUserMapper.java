package com.power.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.power.auth.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
            INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND m.deleted = 0
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
