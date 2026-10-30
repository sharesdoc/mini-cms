package com.cms.common;

/**
 * 业务异常：携带 ResultCode 与自定义消息，由全局处理器转成统一响应。
 */
public class BizException extends RuntimeException {
    private final ResultCode code;

    public BizException(ResultCode code) {
        super(code.msg);
        this.code = code;
    }

    public BizException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public ResultCode getCode() { return code; }
}
