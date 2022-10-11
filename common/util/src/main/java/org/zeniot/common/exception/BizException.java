package org.zeniot.common.exception;

/**
 * @author Wu.Chunyang
 */
public class BizException extends RuntimeException{
    private static final long serialVersionUID = -4429158831713664835L;

    private int errorCode;

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
