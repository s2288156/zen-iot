package org.zen.iot.data.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zen.iot.common.exception.BizException;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Response extends DTO {
    private boolean success;
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

    protected Response(boolean success, int code, String msg) {
        this.success = success;
        this.code = code;
        this.msg = msg;
    }

    public static Response failure(BizException ex) {
        return new Response(false, ex.getErrorCode(), ex.getMessage());
    }
}
