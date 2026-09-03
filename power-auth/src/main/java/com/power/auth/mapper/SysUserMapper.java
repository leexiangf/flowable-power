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

    @Select("""
            SELECT DISTINCT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0
              AND r.status = 1
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT u.id
            FROM sys_user u
            INNER JOIN sys_user_role ur ON u.id = ur.user_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE r.role_code = #{roleCode}
              AND u.deleted = 0
              AND u.status = 1
              AND r.deleted = 0
              AND r.status = 1
            """)
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
