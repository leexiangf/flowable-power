package com.power.auth.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关联。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    private Long userId;
    private Long roleId;
}
