package org.zeniot.dao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zeniot.data.domain.account.Account;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
@Setter
@RequiredArgsConstructor
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
    private Set<RoleEntity> roles = new LinkedHashSet<>();

    public static AccountEntity toEntity(PasswordEncoder encoder, Account account) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(account.getUsername());
        accountEntity.setPassword(encoder.encode(account.getPassword()));
        return accountEntity;
    }

    public Account toAccount() {
        Set<String> roleNames = roles
                .stream()
                .map(roleEntity -> roleEntity.getRoleName().name())
                .collect(Collectors.toSet());
        return new Account(this.username, this.password, roleNames);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AccountEntity that = (AccountEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
