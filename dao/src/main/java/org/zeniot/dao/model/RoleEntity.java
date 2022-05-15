package org.zeniot.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * @author Wu.Chunyang
 */
@Entity
@Table(name = "role")
public class RoleEntity extends BaseEntity {
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    public RoleEntity() {
    }

    public RoleEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleEntity)) return false;
        RoleEntity roleEntity = (RoleEntity) o;
        return Objects.equals(name, roleEntity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}