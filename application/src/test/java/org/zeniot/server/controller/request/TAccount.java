package org.zeniot.server.controller.request;

import lombok.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Data
public class TAccount implements Serializable {
    private static final long serialVersionUID = -6536667040345207843L;

    private String username;

    private String password;

    private Set<String> roles;

    public static TAccount admin() {
        TAccount tAccount = new TAccount();
        tAccount.setUsername("test_admin");
        tAccount.setPassword("123123");
        return tAccount;
    }
}
