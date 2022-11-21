package org.zeniot.server.vo;

import lombok.Getter;
import lombok.Setter;
import org.zeniot.data.base.DTO;

/**
 * @author Wu.Chunyang
 */
@Setter
@Getter
public class UserToken extends DTO {

    private String token;

    public UserToken(String token) {
        this.token = token;
    }
}
