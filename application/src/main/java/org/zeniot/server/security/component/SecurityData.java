package org.zeniot.server.security.component;

import org.springframework.stereotype.Component;
import org.zeniot.data.enums.RoleEnum;

/**
 * @author Wu.Chunyang
 */
@Component(value = "sd")
public class SecurityData {
    public RoleEnum role;
}
