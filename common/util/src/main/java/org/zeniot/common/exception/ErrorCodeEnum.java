package org.zeniot.common.exception;

/**
 * @author Wu.Chunyang
 */

public enum ErrorCodeEnum {
    AUTHENTICATION(40000),
    JWT_TOKEN_EXPIRED(40001);

    private final int code;

    ErrorCodeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
