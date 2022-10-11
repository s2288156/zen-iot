package org.zeniot.dto.core;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Wu.Chunyang
 */
@Getter
public class RestResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -7560501398215693992L;
    private String msg;
    private T data;

    public RestResponse() {
    }

    private RestResponse(String msg) {
        this.msg = msg;
    }

    private RestResponse(T data) {
        this.data = data;
    }

    public static <T> RestResponse<T> ok() {
        return new RestResponse<>("Ok!");
    }

    public static <T> RestResponse<T> ok(String msg) {
        return new RestResponse<>(msg);
    }

    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(data);
    }

    public static <T> RestResponse<T> failure(String msg) {
        return new RestResponse<>(msg);
    }
}
