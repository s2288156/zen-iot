package org.zen.iot.server.controller.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@NoArgsConstructor
@Data
public class TAccount implements Serializable {
    @Serial
    private static final long serialVersionUID = -6536667040345207843L;

    private Long accountId;

    private String username;

    private String password;

    private Set<String> roles;

    public TAccount(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static TAccount admin() {
        return new TAccount("test_admin", "123123");
    }

    public static List<TAccount> accountList() {
        List<TAccount> accountList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            accountList.add(new TAccount("test_admin_" + i, "123123"));
        }
        return accountList;
    }
}
