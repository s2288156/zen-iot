package com.zen.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.admin.entity.RoleEntity;
import com.zen.admin.entity.UserEntity;
import com.zen.common.security.auth.UserContext;
import com.zen.common.security.auth.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户仓储的真库行为：唯一性探测、Specification 分页 + 状态过滤、{@code t_user_role} 覆盖式更新、
 * 逻辑删除后「隐身」、以及审计列 {@code updater} 随 {@link UserContext} 写入（单测的 mock 仓储观察不到 auditing，
 * 「重置口令更新密文与 updater」的后半段只能在这里断言）。
 *
 * <p>native 查询（softDelete）绕过 Hibernate 的自动 flush，写入统一用 {@code saveAndFlush} 先行落盘；
 * {@link Transactional} 让全部探针数据随测试事务回滚。
 */
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
@Transactional
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void existsByUsernameFindsSeedUsersAndIgnoresUnknown() {
        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(userRepository.existsByUsername("no-such-user")).isFalse();
    }

    @Test
    void specificationPageFiltersByStatusAndSortsByIdAsc() {
        userRepository.saveAndFlush(user("it-user-disabled", (byte) 0, "it-u1"));
        UserEntity enabledProbe =
                userRepository.saveAndFlush(user("it-user-enabled", UserEntity.STATUS_ENABLED, "it-u2"));
        entityManager.clear();

        Specification<UserEntity> enabledOnly =
                (root, cq, builder) -> builder.equal(root.get("status"), UserEntity.STATUS_ENABLED);
        Page<UserEntity> page = userRepository.findAll(
                enabledOnly, PageRequest.of(0, 100, Sort.by("id").ascending()));

        assertThat(page.getContent())
                .allSatisfy(entity -> assertThat(entity.getStatus()).isEqualTo(UserEntity.STATUS_ENABLED));
        assertThat(page.getContent())
                .extracting(UserEntity::getUsername)
                .contains("admin", "demo", enabledProbe.getUsername());
        assertThat(page.getContent()).extracting(UserEntity::getUsername).doesNotContain("it-user-disabled");
    }

    @Test
    void rolesAssignmentReplacesPreviousSetOnReload() {
        RoleEntity adminRole = roleRepository.findByRoleCode("admin").orElseThrow();
        RoleEntity userRole = roleRepository.findByRoleCode("user").orElseThrow();

        UserEntity entity = user("it-user-roles", UserEntity.STATUS_ENABLED, null);
        entity.setRoles(new LinkedHashSet<>(List.of(adminRole)));
        userRepository.saveAndFlush(entity);
        entityManager.clear();

        UserEntity reloaded = userRepository.findByUsername("it-user-roles").orElseThrow();
        assertThat(reloaded.getRoles()).extracting(RoleEntity::getRoleCode).containsExactly("admin");

        // 覆盖式分配：整体替换集合后重写，旧关联行必须消失而不是叠加
        reloaded.setRoles(new LinkedHashSet<>(List.of(userRole, adminRole)));
        userRepository.saveAndFlush(reloaded);
        entityManager.clear();

        assertThat(userRepository.findByUsername("it-user-roles").orElseThrow().getRoles())
                .extracting(RoleEntity::getRoleCode)
                .containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void softDeleteHidesUserFromEveryLookup() {
        UserEntity entity = userRepository.saveAndFlush(user("it-user-soft-delete", UserEntity.STATUS_ENABLED, null));

        assertThat(userRepository.softDeleteById(entity.getId())).isEqualTo(1);
        // 已删行的重复删除不再命中：WHERE 条件带 deleted = 0
        assertThat(userRepository.softDeleteById(entity.getId())).isZero();
        entityManager.clear();

        assertThat(userRepository.findById(entity.getId())).isEmpty();
        assertThat(userRepository.existsByUsername("it-user-soft-delete")).isFalse();
    }

    @Test
    void passwordRotationRecordsUpdaterFromUserContext() {
        UserContext.set(new UserPrincipal(1L, "admin", List.of("admin"), List.of("admin"), "jti", null));
        userRepository.saveAndFlush(user("it-user-updater", UserEntity.STATUS_ENABLED, null));
        entityManager.clear();

        UserEntity reloaded = userRepository.findByUsername("it-user-updater").orElseThrow();
        reloaded.setPassword("$2a$10$rotated-rotated-rotated-rotated-rotated-rota");
        userRepository.saveAndFlush(reloaded);

        assertThat(reloaded.getUpdater()).isEqualTo("admin");
    }

    private static UserEntity user(String username, byte status, String nickname) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword("$2a$10$not-a-real-hash-not-a-real-hash-not-a-real-hash-n");
        user.setStatus(status);
        user.setNickname(nickname);
        return user;
    }
}
