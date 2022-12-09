package org.zeniot.dao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zeniot.data.domain.account.Account;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Data
@ToString(callSuper = true)
@Entity
@Table(name = "t_account")
public class AccountEntity extends BaseEntity {

    @Column(name = "username", nullable = false, length = 32)
    private String username;

    @Column(name = "pwd", nullable = false, length = 64)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH})
    @JoinTable(name = "t_account_role",
            joinColumns = @JoinColumn(name = "account_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Set<RoleEntity> roleEntities = new LinkedHashSet<>();

    public static AccountEntity toEntity(PasswordEncoder encoder, Account account) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(account.getUsername());
        accountEntity.setPassword(encoder.encode(account.getPassword()));
        return accountEntity;
    }

    public Account toAccount() {
        Account account = new Account();
        account.setUsername(this.getUsername());
        account.setPassword(this.getPassword());
        account.setRoles(roleEntities
                .stream()
                .map(roleEntity -> roleEntity.getRoleName().name())
                .collect(Collectors.toSet())
        );
        return account;
    }
}
