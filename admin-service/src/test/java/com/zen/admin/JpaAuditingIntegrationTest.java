package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.admin.entity.UserEntity;
import com.zen.admin.repository.UserRepository;
import com.zen.common.core.security.UserContext;
import com.zen.common.core.security.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证「登录用户写库时 {@code creator} 是他的用户名而不是 {@code system}」这条验收标准：审计人取自 {@link UserContext}，要求
 * common-core 的默认 {@code AuditorAware} 确实被本服务的 Bean 覆盖。
 *
 * <p>{@link Transactional} 让测试事务回滚，探针数据不落库。关闭 Nacos 注册，测试不需要注册中心。
 */
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
@Transactional
class JpaAuditingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void createdByMatchesTheLoggedInUser() {
        UserContext.set(new UserPrincipal(2L, "demo", List.of("user"), List.of("ecs"), "jti", null));

        UserEntity saved = userRepository.save(auditProbe());

        assertThat(saved.getCreator()).isEqualTo("demo");
        assertThat(saved.getCreateTime()).isNotNull();
    }

    @Test
    void unauthenticatedWriteFallsBackToNoAuditor() {
        UserEntity saved = userRepository.save(auditProbe());

        assertThat(saved.getCreator()).isNull();
    }

    private UserEntity auditProbe() {
        UserEntity user = new UserEntity();
        user.setUsername("audit-probe");
        user.setPassword("$2a$10$not-a-real-hash");
        user.setStatus(UserEntity.STATUS_ENABLED);
        return user;
    }
}
