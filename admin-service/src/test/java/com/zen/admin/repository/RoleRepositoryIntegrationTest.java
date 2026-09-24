package com.zen.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.admin.entity.RoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色仓储的真库行为：唯一性探测、{@code t_role_module} 关联写入、逻辑删除后「隐身」、被引用计数。
 *
 * <p>native 查询（softDelete/count）绕过 Hibernate 的自动 flush，写入统一用 {@code saveAndFlush} 先行落盘；
 * {@link Transactional} 让全部探针数据随测试事务回滚。
 */
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
@Transactional
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class RoleRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void existsByRoleCodeFindsSeedRoleAndIgnoresUnknownCode() {
        assertThat(roleRepository.existsByRoleCode("admin")).isTrue();
        assertThat(roleRepository.existsByRoleCode("no-such-role")).isFalse();
    }

    @Test
    void modulesAreWrittenToRoleModuleJoinTable() {
        roleRepository.saveAndFlush(role("it-role-modules", "集成测试角色", "wcs", "rcs"));
        entityManager.clear();

        RoleEntity reloaded = roleRepository.findByRoleCode("it-role-modules").orElseThrow();
        assertThat(reloaded.getModules()).containsExactlyInAnyOrder("wcs", "rcs");
    }

    @Test
    void softDeleteHidesRoleFromEveryLookup() {
        RoleEntity role = roleRepository.saveAndFlush(role("it-role-soft-delete", "待删角色", "ecs"));

        assertThat(roleRepository.softDeleteById(role.getId())).isEqualTo(1);
        // 已删行的重复删除不再命中：WHERE 条件带 deleted = 0
        assertThat(roleRepository.softDeleteById(role.getId())).isZero();
        entityManager.clear();

        assertThat(roleRepository.findById(role.getId())).isEmpty();
        assertThat(roleRepository.existsByRoleCode("it-role-soft-delete")).isFalse();
    }

    @Test
    void countUserReferencesSeesSeedUsersButNotNewOrphanRole() {
        RoleEntity seeded = roleRepository.findByRoleCode("admin").orElseThrow();
        assertThat(roleRepository.countUserReferences(seeded.getId())).isGreaterThan(0);

        RoleEntity orphan = roleRepository.saveAndFlush(role("it-role-no-users", "无主角色"));
        assertThat(roleRepository.countUserReferences(orphan.getId())).isZero();
    }

    private static RoleEntity role(String roleCode, String roleName, String... modules) {
        RoleEntity role = new RoleEntity();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setModules(new LinkedHashSet<>(Set.of(modules)));
        return role;
    }
}
