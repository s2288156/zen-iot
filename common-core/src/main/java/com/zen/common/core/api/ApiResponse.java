package com.zen.common.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zen.common.core.exception.ErrorCode;
import com.zen.common.core.exception.GlobalErrorCode;
import lombok.Getter;

/** 统一 API 响应体:code=200 表示成功;失败时为 HTTP 语义码或业务错误码。 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private final int code;

  private final String message;

  private final T data;

  private ApiResponse(int code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public static ApiResponse<Void> success() {
    return new ApiResponse<>(
        GlobalErrorCode.SUCCESS.getCode(), GlobalErrorCode.SUCCESS.getMessage(), null);
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(
        GlobalErrorCode.SUCCESS.getCode(), GlobalErrorCode.SUCCESS.getMessage(), data);
  }

  public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
    return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
  }

  public static <T> ApiResponse<T> failure(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
