package com.power.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "ok"),

    BAD_REQUEST(10000, "请求参数错误"),
    VALIDATION_FAILED(10001, "参数校验失败"),
    UNAUTHORIZED(10002, "未认证"),
    FORBIDDEN(10003, "无权限"),
    NOT_FOUND(10004, "资源不存在"),
    SYSTEM_ERROR(10005, "系统繁忙"),

    AUTH_LOGIN_FAILED(20000, "用户名或密码错误"),
    AUTH_TOKEN_INVALID(20001, "Token 无效或已过期"),
    AUTH_USER_DISABLED(20002, "用户已禁用"),
    AUTH_REFRESH_INVALID(20003, "刷新令牌无效");

    private final int code;
    private final String message;
}
