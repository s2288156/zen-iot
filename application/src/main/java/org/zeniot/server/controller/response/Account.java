package org.zeniot.server.controller.response;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.ToString;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;

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

    private String username;
    private String password;
    private Set<String> roles;

    public AccountEntity toEntity() {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setUsername(username);
        accountEntity.setPassword(password);
        Set<RoleEntity> roleEntities = roles.stream().map(RoleEntity::new).collect(Collectors.toSet());
        accountEntity.setRoles(roleEntities);
        return accountEntity;
    }
}
