package org.zeniot.dto.account;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zeniot.dao.model.AccountEntity;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Wu.Chunyang
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@ToString
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = -6536667040345207843L;

    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private Set<String> roles;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public AccountEntity toEntity(PasswordEncoder passwordEncoder) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(username);
        accountEntity.setPassword(passwordEncoder.encode(password));
        return accountEntity;
    }

    public static Account fromEntity(AccountEntity accountEntity) {
        Account account = new Account();
        account.setUsername(accountEntity.getUsername());
        account.setPassword(accountEntity.getPassword());
        account.setRoles(accountEntity.getRoles()
                .stream()
                .map(roleEntity -> roleEntity.getRoleName().name())
                .collect(Collectors.toSet())
        );
        return account;
    }

    public static Account simpleAccountFromEntity(AccountEntity accountEntity) {
        Account account = new Account();
        account.setId(accountEntity.getId());
        account.setUsername(accountEntity.getUsername());
        account.setCreateTime(accountEntity.getCreateTime());
        account.setUpdateTime(accountEntity.getUpdateTime());
        return account;
    }
}
