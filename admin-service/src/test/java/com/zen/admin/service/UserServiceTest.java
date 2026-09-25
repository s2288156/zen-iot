package com.zen.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zen.admin.dto.UserCreateRequest;
import com.zen.admin.dto.UserQuery;
import com.zen.admin.dto.UserResetPasswordRequest;
import com.zen.admin.dto.UserStatusRequest;
import com.zen.admin.dto.UserUpdateRequest;
import com.zen.admin.dto.UserView;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.entity.UserEntity;
import com.zen.admin.repository.RoleRepository;
import com.zen.admin.repository.UserRepository;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link UserService} 的纯单元测试：仓储用 Mockito mock，PasswordEncoder 用真实的 BCrypt 实现——
 * 「口令以密文落库」是验收语义，mock 掉编码器就只能断言 wiring，断言不了加密。
 *
 * <p>覆盖验收语义：BCrypt 密文、{@code username} 冲突 409、角色覆盖式分配与未知 roleId 400、重置口令替换密文、
 * 排序白名单。updater 审计断言不在这里做——JPA auditing 只在真实 flush 时生效，mock 仓储观察不到，
 * 由 {@code UserRepositoryIntegrationTest} 兜底。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String RAW_PASSWORD = "Initial@123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    // ---------- create ----------

    @Test
    void createStoresBcryptHashAndEnablesUser() {
        when(userRepository.existsByUsername("wcs-operator")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        UserView view = userService.create(
                new UserCreateRequest("wcs-operator", RAW_PASSWORD, "张三", "zhangsan@example.com", "13800000000", null));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity stored = captor.getValue();
        assertThat(stored.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored.getPassword())).isTrue();
        assertThat(stored.getStatus()).isEqualTo(UserEntity.STATUS_ENABLED);
        assertThat(view.id()).isEqualTo(7L);
        assertThat(view.username()).isEqualTo("wcs-operator");
        assertThat(view.nickname()).isEqualTo("张三");
        // 视图绝不回传口令：record 本身就没有 password 组件，这里只核对资料映射
        assertThat(view.avatar()).isNull();
    }

    @Test
    void createWithDuplicateUsernameThrowsConflict() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(
                        () -> userService.create(new UserCreateRequest("admin", RAW_PASSWORD, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).contains("admin");
                });
        verify(userRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void updateChangesProfileFieldsOnly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "demo")));

        UserView view = userService.update(
                1L, new UserUpdateRequest("演示账号", "demo@example.com", null, "https://cdn.example.com/a.png"));

        assertThat(view.username()).isEqualTo("demo");
        assertThat(view.nickname()).isEqualTo("演示账号");
        assertThat(view.email()).isEqualTo("demo@example.com");
        assertThat(view.phone()).isNull();
        assertThat(view.avatar()).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void updateMissingUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, new UserUpdateRequest("昵称", null, null, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
    }

    // ---------- changeStatus ----------

    @Test
    void changeStatusDisablesAndEnables() {
        UserEntity entity = user(1L, "demo");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(userService.changeStatus(1L, new UserStatusRequest(0)).status())
                .isEqualTo(0);
        assertThat(entity.getStatus()).isZero();

        assertThat(userService.changeStatus(1L, new UserStatusRequest(1)).status())
                .isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(UserEntity.STATUS_ENABLED);
    }

    // ---------- delete ----------

    @Test
    void deleteSoftDeletesWithoutRoleReferenceCheck() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "demo")));

        userService.delete(1L);

        verify(userRepository).softDeleteById(1L);
        // 固化「有意不对称」：删用户不查角色引用（对照 RoleService.delete 的 409），roleRepository 必须全程零交互
        verifyNoInteractions(roleRepository);
    }

    @Test
    void deleteMissingUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
        verify(userRepository, never()).softDeleteById(any());
    }

    // ---------- resetPassword ----------

    @Test
    void resetPasswordReplacesHashWithNewOne() {
        UserEntity entity = user(1L, "demo");
        entity.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        userService.resetPassword(1L, new UserResetPasswordRequest("Rotated@456"));

        assertThat(passwordEncoder.matches("Rotated@456", entity.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, entity.getPassword())).isFalse();
    }

    @Test
    void resetPasswordMissingUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(99L, new UserResetPasswordRequest("Rotated@456")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
    }

    // ---------- assignRoles ----------

    @Test
    void assignRolesReplacesEntireSetAndSortsViewIds() {
        UserEntity entity = user(1L, "demo");
        entity.setRoles(new LinkedHashSet<>(List.of(role(2L))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(roleRepository.findAllById(Set.of(1L, 3L))).thenReturn(List.of(role(3L), role(1L)));

        UserView view = userService.assignRoles(1L, new LinkedHashSet<>(List.of(3L, 1L)));

        // 视图 roleIds 升序，避免响应漂移
        assertThat(view.roleIds()).containsExactly(1L, 3L);
        assertThat(entity.getRoles()).extracting(RoleEntity::getId).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void assignRolesWithEmptySetClearsEverything() {
        UserEntity entity = user(1L, "demo");
        entity.setRoles(new LinkedHashSet<>(List.of(role(2L))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(userService.assignRoles(1L, Set.of()).roleIds()).isEmpty();
        verifyNoInteractions(roleRepository);
    }

    @Test
    void assignRolesWithUnknownRoleIdThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "demo")));
        when(roleRepository.findAllById(any())).thenReturn(List.of(role(1L)));

        assertThatThrownBy(() -> userService.assignRoles(1L, Set.of(1L, 99L)))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("99");
                });
    }

    @Test
    void assignRolesWithNullElementThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "demo")));
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(null);

        assertThatThrownBy(() -> userService.assignRoles(1L, roleIds))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST));
        verifyNoInteractions(roleRepository);
    }

    // ---------- page ----------

    @Test
    void pageDefaultsToIdAscAndMapsEntitiesToViews() {
        UserQuery query = new UserQuery();
        when(userRepository.findAll(ArgumentMatchers.<Specification<UserEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L, "admin"), user(2L, "demo")), PageRequest.of(0, 10), 2));

        PageResult<UserView> result = userService.page(query);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(ArgumentMatchers.<Specification<UserEntity>>any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("id"))
                .isNotNull()
                .satisfies(order -> assertThat(order.getDirection().name()).isEqualTo("ASC"));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).extracting(UserView::username).containsExactly("admin", "demo");
    }

    @Test
    void pageRejectsOrderByOutsideWhitelist() {
        UserQuery query = new UserQuery();
        query.setOrderBy("password");

        assertThatThrownBy(() -> userService.page(query)).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST);
            assertThat(ex.getMessage()).contains("password");
        });
        verify(userRepository, never()).findAll(ArgumentMatchers.<Specification<UserEntity>>any(), any(Pageable.class));
    }

    // ---------- detail ----------

    @Test
    void detailReturnsViewForActiveUser() {
        UserEntity entity = user(1L, "demo");
        entity.setRoles(new LinkedHashSet<>(List.of(role(2L))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        UserView view = userService.detail(1L);

        assertThat(view.username()).isEqualTo("demo");
        assertThat(view.roleIds()).containsExactly(2L);
    }

    @Test
    void detailMissingUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.detail(99L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
    }

    // ---------- fixtures ----------

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$placeholder-placeholder-placeholder-placeholder-pla");
        user.setStatus(UserEntity.STATUS_ENABLED);
        return user;
    }

    private static RoleEntity role(long id) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        return role;
    }
}
