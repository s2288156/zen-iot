package org.zen.iot.data.vo;

import lombok.Getter;
import lombok.Setter;
import org.zen.iot.data.base.DTO;

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
