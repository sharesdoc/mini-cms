package com.cms.common;

/**
 * 统一业务错误码（与 G-系统接口清单.md §8 对齐）。
 */
public enum ResultCode {

    OK(0, "ok"),
    BAD_PARAM(40001, "bad request"),
    UNAUTHORIZED(40101, "unauthorized"),
    FORBIDDEN(40301, "forbidden"),
    USER_EXISTS(40901, "username already exists"),
    USER_NOT_FOUND(40902, "username not found"),
    PWD_WRONG(40903, "incorrect password"),
    USER_DISABLED(40904, "account disabled"),
    EXPAND_FAIL(40905, "expand validation failed"),
    FRIEND_FAIL(40906, "friend validation failed"),
    GROUP_FAIL(40907, "group validation failed"),
    SERVER_ERROR(50000, "server error");

    public final int code;
    public final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
