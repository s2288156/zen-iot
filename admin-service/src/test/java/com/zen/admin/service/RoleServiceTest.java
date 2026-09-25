package com.zen.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zen.admin.dto.RoleCreateRequest;
import com.zen.admin.dto.RoleQuery;
import com.zen.admin.dto.RoleUpdateRequest;
import com.zen.admin.dto.RoleView;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.repository.RoleRepository;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * {@link RoleService} 的纯单元测试：仓储用 Mockito mock，不启动 Spring 上下文。
 *
 * <p>覆盖三条验收语义：{@code roleCode} 冲突 409、非法模块编码 400、删除被引用角色 409；外加排序白名单与
 * {@code assignModules} 的覆盖式替换。
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    // ---------- create ----------

    @Test
    void createPersistsValidatedModulesAndReturnsSortedView() {
        when(roleRepository.existsByRoleCode("wcs-operator")).thenReturn(false);
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(invocation -> {
            RoleEntity saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        RoleView view =
                roleService.create(new RoleCreateRequest("wcs-operator", "WCS 操作员", "负责站点分配", Set.of("wcs", "admin")));

        assertThat(view.id()).isEqualTo(7L);
        assertThat(view.roleCode()).isEqualTo("wcs-operator");
        // 视图里的 modules 按字典序，避免响应漂移
        assertThat(view.modules()).containsExactly("admin", "wcs");
        verify(roleRepository)
                .save(argThat(entity -> "wcs-operator".equals(entity.getRoleCode())
                        && entity.getModules().equals(Set.of("wcs", "admin"))));
    }

    @Test
    void createWithDuplicateRoleCodeThrowsConflict() {
        when(roleRepository.existsByRoleCode("admin")).thenReturn(true);

        assertThatThrownBy(() -> roleService.create(new RoleCreateRequest("admin", "重复管理员", null, Set.of())))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).contains("admin");
                });
        verify(roleRepository, never()).save(any());
    }

    @Test
    void createWithUnknownModuleCodeThrowsBadRequest() {
        when(roleRepository.existsByRoleCode("new-role")).thenReturn(false);

        assertThatThrownBy(() -> roleService.create(new RoleCreateRequest("new-role", "新角色", null, Set.of("ghost"))))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST));
        verify(roleRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void updateChangesDisplayNameAndDescriptionOnly() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "wcs-operator", "WCS 操作员")));

        RoleView view = roleService.update(1L, new RoleUpdateRequest("WCS 高级操作员", "含配置维护权限"));

        assertThat(view.roleName()).isEqualTo("WCS 高级操作员");
        assertThat(view.description()).isEqualTo("含配置维护权限");
        assertThat(view.roleCode()).isEqualTo("wcs-operator");
    }

    @Test
    void updateMissingRoleThrowsNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.update(99L, new RoleUpdateRequest("名字", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
    }

    // ---------- delete ----------

    @Test
    void deleteSoftDeletesUnreferencedRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "spare", "备用角色")));
        when(roleRepository.countUserReferences(1L)).thenReturn(0L);

        roleService.delete(1L);

        verify(roleRepository).softDeleteById(1L);
    }

    @Test
    void deleteReferencedByUsersThrowsConflictAndKeepsRow() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "admin", "管理员")));
        when(roleRepository.countUserReferences(1L)).thenReturn(2L);

        assertThatThrownBy(() -> roleService.delete(1L)).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.CONFLICT);
            assertThat(ex.getMessage()).contains("引用");
        });
        verify(roleRepository, never()).softDeleteById(any());
    }

    @Test
    void deleteMissingRoleThrowsNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.delete(99L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
    }

    // ---------- page ----------

    @Test
    void pageDefaultsToIdAscAndMapsEntitiesToViews() {
        RoleQuery query = new RoleQuery();
        when(roleRepository.findAll(ArgumentMatchers.<Specification<RoleEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(role(1L, "admin", "管理员"), role(2L, "user", "普通用户")), PageRequest.of(0, 10), 2));

        PageResult<RoleView> result = roleService.page(query);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(roleRepository).findAll(ArgumentMatchers.<Specification<RoleEntity>>any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("id"))
                .isNotNull()
                .satisfies(order -> assertThat(order.getDirection().name()).isEqualTo("ASC"));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).extracting(RoleView::roleCode).containsExactly("admin", "user");
    }

    @Test
    void pageRejectsOrderByOutsideWhitelist() {
        RoleQuery query = new RoleQuery();
        query.setOrderBy("creator");

        assertThatThrownBy(() -> roleService.page(query)).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST);
            assertThat(ex.getMessage()).contains("creator");
        });
        verify(roleRepository, never()).findAll(ArgumentMatchers.<Specification<RoleEntity>>any(), any(Pageable.class));
    }

    // ---------- detail ----------

    @Test
    void detailReturnsViewForActiveRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "admin", "管理员")));

        assertThat(roleService.detail(1L).roleCode()).isEqualTo("admin");
    }

    @Test
    void detailMissingRoleThrowsNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.detail(99L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND));
    }

    // ---------- assignModules ----------

    @Test
    void assignModulesReplacesEntireSet() {
        RoleEntity entity = role(1L, "wcs-operator", "WCS 操作员");
        entity.setModules(new LinkedHashSet<>(Set.of("admin", "rcs")));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(entity));

        RoleView view = roleService.assignModules(1L, Set.of("wcs"));

        assertThat(view.modules()).containsExactly("wcs");
    }

    @Test
    void assignModulesWithEmptySetRevokesEverything() {
        RoleEntity entity = role(1L, "wcs-operator", "WCS 操作员");
        entity.setModules(new LinkedHashSet<>(Set.of("wcs")));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(roleService.assignModules(1L, Set.of()).modules()).isEmpty();
    }

    @Test
    void assignModulesWithUnknownCodeThrowsBadRequest() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "wcs-operator", "WCS 操作员")));

        assertThatThrownBy(() -> roleService.assignModules(1L, Set.of("WCS")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST));
    }

    // ---------- fixtures ----------

    private static RoleEntity role(long id, String roleCode, String roleName) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        return role;
    }
}
