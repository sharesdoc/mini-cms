package com.cms.common;

/**
 * 统一响应体：{code, msg, data}。
 */
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public Result() {}

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.OK.code, ResultCode.OK.msg, null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.OK.code, ResultCode.OK.msg, data);
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return new Result<>(rc.code, rc.msg, null);
    }

    public static <T> Result<T> fail(ResultCode rc, String msg) {
        return new Result<>(rc.code, msg, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
