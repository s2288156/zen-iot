package org.zeniot.server.controller.response;

import lombok.Getter;

/**
 * @author Wu.Chunyang
 */
@Getter
public class RestResponse<T> {
    private String msg;
    private T data;

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
