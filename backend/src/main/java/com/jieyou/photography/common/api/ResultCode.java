package com.jieyou.photography.common.api;

/**
 * 业务错误码常量，依据 docs/API接口约定.md 2.5 节。
 */
public final class ResultCode {

    public static final int SUCCESS = 0;
    public static final int PARAM_INVALID = 40000;
    public static final int UNAUTHORIZED = 40100;
    public static final int FORBIDDEN = 40300;
    public static final int NOT_FOUND = 40400;
    public static final int STATE_CONFLICT = 40900;
    public static final int DUPLICATE = 40901;
    public static final int SERVER_ERROR = 50000;

    private ResultCode() {
    }

    /**
     * 业务码到 HTTP 状态码的映射（API 约定 2.4）。
     */
    public static int httpStatus(int code) {
        return switch (code) {
            case SUCCESS -> 200;
            case PARAM_INVALID -> 400;
            case UNAUTHORIZED -> 401;
            case FORBIDDEN -> 403;
            case NOT_FOUND -> 404;
            case STATE_CONFLICT, DUPLICATE -> 409;
            default -> 500;
        };
    }
}
