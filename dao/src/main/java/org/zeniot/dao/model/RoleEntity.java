package org.zeniot.dao.model;

import lombok.ToString;
import org.zeniot.data.enums.RoleEnum;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author Wu.Chunyang
 */
@ToString(callSuper = true)
@Entity
@Table(name = "t_role")
public class RoleEntity extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 64)
    private RoleEnum roleName;

    public RoleEntity() {
    }

    public RoleEntity(RoleEnum role) {
        this.roleName = role;
    }

    public RoleEnum getRoleName() {
        return roleName;
    }

    public void setRoleName(RoleEnum roleName) {
        this.roleName = roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleEntity)) return false;
        RoleEntity roleEntity = (RoleEntity) o;
        return Objects.equals(roleName, roleEntity.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleName);
    }
}
