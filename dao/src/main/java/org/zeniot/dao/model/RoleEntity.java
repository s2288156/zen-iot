package org.zeniot.dao.model;

import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * @author Wu.Chunyang
 */
@ToString(callSuper = true)
@Entity
@Table(name = "t_role")
public class RoleEntity extends BaseEntity {
    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    public RoleEntity() {
    }

    public RoleEntity(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String name) {
        this.roleName = name;
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
