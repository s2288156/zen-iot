package org.zeniot.server.controller.response;

import lombok.Getter;

/**
 * @author Wu.Chunyang
 */
@Getter
public class RestResponse {
    private String msg;

    public RestResponse(String msg) {
        this.msg = msg;
    }

    public static RestResponse ok() {
        return new RestResponse("Ok!");
    }

    public static RestResponse ok(String msg) {
        return new RestResponse(msg);
    }

    public static RestResponse failure(String msg) {
        return new RestResponse(msg);
    }
}
