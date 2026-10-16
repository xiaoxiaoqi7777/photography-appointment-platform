package com.jieyou.photography.common.exception;

import com.jieyou.photography.common.api.ResultCode;

/**
 * 业务异常：携带业务错误码，由全局异常处理器转换为统一响应。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException() {
        this(ResultCode.SERVER_ERROR, "服务器内部错误");
    }

    public int getCode() {
        return code;
    }
}
