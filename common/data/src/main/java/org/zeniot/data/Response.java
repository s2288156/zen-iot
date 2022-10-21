package org.zeniot.data;

import lombok.Data;

/**
 * @author Wu.Chunyang
 */
@Data
public class Response extends DTO {
    private boolean success;
    // TODO: 10/21/2022 支持BizEx
    private int code;
    private String msg;

    protected Response() {
    }

    protected Response(boolean success) {
        this.success = success;
    }

    protected Response(boolean success, String msg) {
        this.success = success;
        this.msg = msg;
    }

    public static Response fail(String msg) {
        return new Response(false, msg);
    }
}
