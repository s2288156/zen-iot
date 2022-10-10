package org.zeniot.server.security.component;

import org.springframework.stereotype.Component;
import org.zeniot.dao.model.RoleEnums;

/**
 * @author Wu.Chunyang
 */
@Component(value = "sd")
public class SecurityData {
    public RoleEnums role;
}
