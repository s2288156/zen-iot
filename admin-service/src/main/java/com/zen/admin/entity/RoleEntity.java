package com.zen.admin.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** 角色：模块访问权的唯一载体。用 Set 而非 List，否则 Hibernate 会为关联表生成序号列，与建表脚本不符。 */
@Getter
@Setter
@Entity
@Table(name = "t_role")
@SQLRestriction("deleted = 0")
public class RoleEntity extends BaseEntity {

  @Column(name = "role_code", nullable = false, length = 64, unique = true)
  private String roleCode;

  @Column(name = "role_name", nullable = false, length = 64)
  private String roleName;

  @Column(name = "description", length = 255)
  private String description;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "t_role_module", joinColumns = @JoinColumn(name = "role_id"))
  @Column(name = "module_code", nullable = false, length = 32)
  private Set<String> modules = new LinkedHashSet<>();
}
