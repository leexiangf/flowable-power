package com.power.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户上下文（Security 主体）。
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 登录平台：WEB / MOBILE */
    private String platform;

    /** 权限码列表 */
    private List<String> authorities = new ArrayList<>();

    /** 是否调试鉴权注入的临时用户 */
    private boolean debug;
}
