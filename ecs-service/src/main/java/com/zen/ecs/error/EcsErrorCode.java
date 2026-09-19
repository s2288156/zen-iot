package com.zen.ecs.error;

import com.zen.common.security.error.ErrorCode;

/**
 * ECS 业务错误码，占用 1xxx 段。
 *
 * <p>与 HTTP 语义码（4xx/5xx）错开后，{@code GlobalExceptionHandler} 会让 HTTP 回落 200、由响应体 code 表达错误， 因此这些码不会伪装成传输层故障。
 */
public enum EcsErrorCode implements ErrorCode {
    DEVICE_NOT_FOUND(1001, "设备不存在"),
    DEVICE_CODE_DUPLICATED(1002, "设备编码已存在"),
    DEVICE_GROUP_NOT_FOUND(1003, "设备分组不存在"),
    DEVICE_GROUP_CODE_DUPLICATED(1004, "设备分组编码已存在"),
    DEVICE_GROUP_IN_USE(1005, "分组下仍有设备，不能删除"),
    PROTOCOL_NOT_SUPPORTED(1006, "设备协议类型没有可用的适配器"),
    COMMAND_NOT_FOUND(1007, "指令不存在");

    private final int code;

    private final String message;

    EcsErrorCode(int code, String message) {
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
