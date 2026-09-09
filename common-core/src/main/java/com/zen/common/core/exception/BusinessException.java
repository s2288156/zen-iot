package com.zen.common.core.exception;

import lombok.Getter;

/** 可预期的业务规则失败。全局异常处理器按 errorCode 透出 code,message 取异常消息。 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
