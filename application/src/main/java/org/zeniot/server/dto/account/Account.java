package org.zeniot.server.dto.account;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zeniot.dao.id.AccountId;
import org.zeniot.dao.model.AccountEntity;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Setter(AccessLevel.PRIVATE)
@ToString
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = -6536667040345207843L;

    @Getter
    private AccountId accountId;

    @NotBlank
    @Getter
    private String username;

    @NotBlank
    private String password;

    private Set<String> roles;

    public AccountEntity toEntity(PasswordEncoder passwordEncoder) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(username);
        accountEntity.setPassword(passwordEncoder.encode(password));
        return accountEntity;
    }

    public static Account simpleAccountFromEntity(AccountEntity accountEntity) {
        Account account = new Account();
        account.setAccountId(AccountId.of(accountEntity.getId()));
        account.setUsername(accountEntity.getUsername());
        return account;
    }
}
