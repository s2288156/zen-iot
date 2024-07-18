package org.zen.iot.server.security.component;

import org.springframework.stereotype.Component;
import org.zen.iot.data.enums.RoleEnum;

/**
 * @author Wu.Chunyang
 */
@Component(value = "sd")
public class SecurityData {
    public RoleEnum role;
}
