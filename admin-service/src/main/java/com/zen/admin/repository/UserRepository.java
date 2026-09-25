package com.zen.admin.repository;

import com.zen.admin.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * 逻辑删除：置 {@code deleted = 1}。
     *
     * <p>走 native UPDATE 的原因同 {@link RoleRepository#softDeleteById}：{@code deleted} 列刻意不映射到实体，
     * 实体侧只靠 {@code @SQLRestriction("deleted = 0")} 过滤。{@code t_user_role} 的关联行保留，
     * 便于审计与误删恢复。
     */
    @Modifying
    @Query(value = "UPDATE t_user SET deleted = 1 WHERE id = :id AND deleted = 0", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);
}
