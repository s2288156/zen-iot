package com.zen.admin.service;

import com.zen.admin.dto.RoleCreateRequest;
import com.zen.admin.dto.RoleQuery;
import com.zen.admin.dto.RoleUpdateRequest;
import com.zen.admin.dto.RoleView;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.repository.RoleRepository;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 角色管理：增删改查 + 模块授权。
 *
 * <p>三条语义要和前端/文档保持一致：{@code roleCode} 冲突与「删除仍被引用的角色」都是 409，但消息不同；
 * 删除是逻辑删除（{@code deleted = 1}），实体上的 {@code @SQLRestriction} 让已删角色对所有查询立即隐身。
 */
@Service
public class RoleService {

    /** 排序白名单：{@code PageQuery.orderBy} 直接来自客户端，必须校验后才允许进入 {@link Pageable}。 */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "roleCode", "roleName", "createTime", "updateTime");

    private static final Set<String> VALID_MODULE_CODES =
            Arrays.stream(ModuleCode.values()).map(ModuleCode::getCode).collect(Collectors.toUnmodifiableSet());

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public RoleView create(RoleCreateRequest request) {
        if (roleRepository.existsByRoleCode(request.roleCode())) {
            throw new BusinessException(GlobalErrorCode.CONFLICT, "角色编码已存在: " + request.roleCode());
        }
        RoleEntity role = new RoleEntity();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setModules(new LinkedHashSet<>(validatedModules(request.modules())));
        return toView(roleRepository.save(role));
    }

    /** 只改展示属性；{@code roleCode} 是业务标识不可改，模块授权走 {@link #assignModules}。 */
    @Transactional
    public RoleView update(Long id, RoleUpdateRequest request) {
        RoleEntity role = requireActive(id);
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        return toView(role);
    }

    /** 逻辑删除。仍被有效用户引用时拒绝（409），避免把用户留在无角色的悬空引用状态。 */
    @Transactional
    public void delete(Long id) {
        RoleEntity role = requireActive(id);
        if (roleRepository.countUserReferences(role.getId()) > 0) {
            throw new BusinessException(GlobalErrorCode.CONFLICT, "角色仍被用户引用,无法删除");
        }
        roleRepository.softDeleteById(role.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<RoleView> page(RoleQuery query) {
        String orderBy = query.getOrderBy();
        if (StringUtils.hasText(orderBy) && !SORTABLE_FIELDS.contains(orderBy)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不支持的排序字段: " + orderBy);
        }
        if (!StringUtils.hasText(orderBy)) {
            // 不给排序条件时按主键兜底，保证翻页稳定
            query.setOrderBy("id");
        }
        Page<RoleEntity> page = roleRepository.findAll(roleCodeAndNameLike(query), query.toPageable());
        return PageResult.of(
                page, page.getContent().stream().map(RoleService::toView).toList());
    }

    @Transactional(readOnly = true)
    public RoleView detail(Long id) {
        return toView(requireActive(id));
    }

    /** 覆盖式授权：提交的集合整体替换 {@code t_role_module} 关联；空集合表示收回全部模块。 */
    @Transactional
    public RoleView assignModules(Long id, @Nullable Set<String> modules) {
        RoleEntity role = requireActive(id);
        role.setModules(new LinkedHashSet<>(validatedModules(modules)));
        return toView(role);
    }

    private RoleEntity requireActive(Long id) {
        return roleRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "角色不存在: " + id));
    }

    private static Set<String> validatedModules(@Nullable Set<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return Set.of();
        }
        Set<String> validated = new LinkedHashSet<>();
        for (String code : modules) {
            if (code == null || !VALID_MODULE_CODES.contains(code)) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "非法模块编码: " + code);
            }
            validated.add(code);
        }
        return validated;
    }

    private static Specification<RoleEntity> roleCodeAndNameLike(RoleQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getRoleCode())) {
                predicates.add(builder.like(root.get("roleCode"), likePattern(query.getRoleCode())));
            }
            if (StringUtils.hasText(query.getRoleName())) {
                predicates.add(builder.like(root.get("roleName"), likePattern(query.getRoleName())));
            }
            return predicates.isEmpty() ? null : builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** 转义 LIKE 通配符，防止用户输入的 {@code %} / {@code _} 扩大匹配范围。 */
    private static String likePattern(String raw) {
        String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static RoleView toView(RoleEntity role) {
        List<String> modules = role.getModules().stream().sorted().toList();
        return new RoleView(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                modules,
                role.getCreator(),
                role.getCreateTime(),
                role.getUpdater(),
                role.getUpdateTime());
    }
}
