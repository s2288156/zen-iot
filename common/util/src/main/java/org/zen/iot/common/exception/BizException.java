package org.zen.iot.common.exception;

import java.io.Serial;

/**
 * @author Wu.Chunyang
 */
public class BizException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = -4429158831713664835L;

    private int errorCode;

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, ErrorCodeEnum errorCode) {
        super(message);
        this.errorCode = errorCode.getCode();
    }

    public int getErrorCode() {
        return errorCode;
    }
}
