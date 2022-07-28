package org.zeniot.server.controller.request;

import lombok.*;
import org.zeniot.dao.id.AccountId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@NoArgsConstructor
@Data
public class TAccount implements Serializable {
    private static final long serialVersionUID = -6536667040345207843L;

    private AccountId accountId;

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
