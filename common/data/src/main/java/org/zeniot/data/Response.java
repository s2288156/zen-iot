package org.zeniot.data;

/**
 * @author Wu.Chunyang
 */
public abstract class Response extends DTO {
    private boolean success;
    private String msg;

    public Response() {
    }

    public Response(String msg) {
        this.msg = msg;
    }
}
