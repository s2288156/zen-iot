package org.zeniot.data;

import lombok.Getter;

/**
 * @author Wu.Chunyang
 */
@Getter
public class SingleResponse<T> extends Response {
    private T data;

    private SingleResponse(String msg) {
        super(msg);
    }

    private SingleResponse(T data) {
        this.data = data;
    }

    public static <T> SingleResponse<T> ok() {
        return new SingleResponse<>("Ok!");
    }

    public static <T> SingleResponse<T> ok(String msg) {
        return new SingleResponse<>(msg);
    }

    public static <T> SingleResponse<T> success(T data) {
        return new SingleResponse<>(data);
    }

    public static <T> SingleResponse<T> failure(String msg) {
        return new SingleResponse<>(msg);
    }
}
