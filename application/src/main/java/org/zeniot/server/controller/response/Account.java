package org.zeniot.server.controller.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Wu.Chunyang
 */
@Setter(AccessLevel.PRIVATE)
@ToString
public class Account implements Serializable {
    private static final long serialVersionUID = -6536667040345207843L;

    @NotBlank
    @Getter
    private String username;

    @NotBlank
    private String password;

    @NotNull(message = "The roles can't be empty!")
    private Set<String> roles;

    public AccountEntity toEntity(PasswordEncoder passwordEncoder) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(username);
        accountEntity.setPassword(passwordEncoder.encode(password));
        Set<RoleEntity> roleEntities = roles.stream().map(RoleEntity::new).collect(Collectors.toSet());
        accountEntity.setRoles(roleEntities);
        return accountEntity;
    }
}
