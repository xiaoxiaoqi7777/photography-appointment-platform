package com.jieyou.photography.common.api;

/**
 * 统一响应结构，依据 docs/API接口约定.md 2.3 节：{ code, message, data }。
 */
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS, "成功", data);
    }

    /**
     * 成功但需要附带可读提示（如“关联主题已下架”），code 仍为 0。
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(ResultCode.SUCCESS, message, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResultCode.SUCCESS, "成功", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
