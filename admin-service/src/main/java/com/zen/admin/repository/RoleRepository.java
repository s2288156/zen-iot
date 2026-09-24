package com.zen.admin.repository;

import com.zen.admin.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<RoleEntity, Long>, JpaSpecificationExecutor<RoleEntity> {

    Optional<RoleEntity> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    /**
     * 逻辑删除：置 {@code deleted = 1}。
     *
     * <p>走 native UPDATE 而不是改实体：{@code deleted} 列按 V1 建表注释刻意不映射到实体，实体侧只靠
     * {@code @SQLRestriction("deleted = 0")} 过滤。{@code t_role_module} 的关联行保留，便于审计与误删恢复。
     */
    @Modifying
    @Query(value = "UPDATE t_role SET deleted = 1 WHERE id = :id AND deleted = 0", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);

    /**
     * 统计仍在引用该角色的有效用户数。
     *
     * <p>{@code t_user_role} 是纯关联表、未映射实体，{@code t_user.deleted} 同样不在实体上，只能 native 查询；
     * 已逻辑删除的用户不算引用。
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM t_user_role ur
            JOIN t_user u ON u.id = ur.user_id
            WHERE ur.role_id = :roleId AND u.deleted = 0
            """, nativeQuery = true)
    long countUserReferences(@Param("roleId") Long roleId);
}
