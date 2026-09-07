package com.zen.common.core.exception;

/** 通用错误码,与 HTTP 语义对齐;业务错误码请在消费方服务内自建枚举实现 {@link ErrorCode}。 */
public enum GlobalErrorCode implements ErrorCode {
  SUCCESS(200, "成功"),
  BAD_REQUEST(400, "请求参数有误"),
  UNAUTHORIZED(401, "未认证或登录已失效"),
  FORBIDDEN(403, "无访问权限"),
  NOT_FOUND(404, "资源不存在"),
  METHOD_NOT_ALLOWED(405, "请求方法不被支持"),
  CONFLICT(409, "资源状态冲突"),
  TOO_MANY_REQUESTS(429, "请求过于频繁,请稍后重试"),
  INTERNAL_SERVER_ERROR(500, "系统异常"),
  NOT_IMPLEMENTED(501, "功能未实现"),
  SERVICE_UNAVAILABLE(503, "服务暂不可用");

  private final int code;

  private final String message;

  GlobalErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
