package com.zen.common.core.exception;

/** 错误码契约。各业务服务可定义自己的枚举实现本接口,扩展独立的业务码段。 */
public interface ErrorCode {

  int getCode();

  String getMessage();
}
