package org.zeniot.data;

import lombok.Getter;

/**
 * @author Wu.Chunyang
 */
@Getter
public class SingleResponse<T> extends Response {
    private T data;

    public SingleResponse(boolean success) {
        super(success);
    }

    public SingleResponse(boolean success, T data) {
        super(success);
        this.data = data;
    }

    public SingleResponse(boolean success, String msg) {
        super(success, msg);
    }

    public static <T> SingleResponse<T> success() {
        return new SingleResponse<>(true);
    }

    public static <T> SingleResponse<T> success(String msg) {
        return new SingleResponse<>(true, msg);
    }

    public static <T> SingleResponse<T> success(T data) {
        return new SingleResponse<>(true, data);
    }

    public static <T> SingleResponse<T> failure(String msg) {
        return new SingleResponse<>(false, msg);
    }
}
