package org.zeniot.dao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.zeniot.data.enums.RoleEnum;

import java.util.Objects;

/**
 * @author Wu.Chunyang
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "t_role")
public class RoleEntity extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 64)
    private RoleEnum roleName;

    public RoleEntity() {
    }

    public RoleEntity(RoleEnum role) {
        this.roleName = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        RoleEntity that = (RoleEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
