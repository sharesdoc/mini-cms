package com.cms.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：将异常统一转换为 Result 响应，避免向前端泄露内部错误细节。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(fe -> fe.getDefaultMessage()).orElse(ResultCode.BAD_PARAM.msg);
        return Result.fail(ResultCode.BAD_PARAM, msg);
    }

    /**
     * 唯一键冲突：并发同用户名注册时，应用层 count 检查与 insert 之间存在竞态，
     * DB 的 UNIQUE 约束兜底数据完整性；此处将冲突转为干净的 40901（用户名已存在），
     * 而非向前端暴露 50000。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("duplicate key: {}", e.getMessage());
        return Result.fail(ResultCode.USER_EXISTS);
    }

    /** 未捕获异常兜底 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("unhandled exception", e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}

