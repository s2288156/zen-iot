package org.zeniot.dto.core;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Wu.Chunyang
 */
@Getter
public class SingleResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -7560501398215693992L;
    private String msg;
    private T data;

    private SingleResponse(String msg) {
        this.msg = msg;
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
