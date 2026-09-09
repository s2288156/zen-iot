package com.zen.admin.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** 用户与登录凭证。权限一律经角色取得，用户不直接持有模块。 */
@Getter
@Setter
@Entity
@Table(name = "t_user")
@SQLRestriction("deleted = 0")
public class UserEntity extends BaseEntity {

  /** 启用状态，对应 t_user.status 的 TINYINT；改成 Integer 会让 ddl-auto=validate 报列类型不符。 */
  public static final byte STATUS_ENABLED = 1;

  @Column(name = "username", nullable = false, length = 64, unique = true)
  private String username;

  @Column(name = "password", nullable = false, length = 100)
  private String password;

  @Column(name = "status", nullable = false)
  private Byte status;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "t_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<RoleEntity> roles = new LinkedHashSet<>();

  public boolean isEnabled() {
    return status != null && status == STATUS_ENABLED;
  }
}
