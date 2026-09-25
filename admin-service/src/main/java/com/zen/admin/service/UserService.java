package com.zen.admin.service;

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
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户管理：建号、资料维护、启停、逻辑删除、重置口令、角色分配。
 *
 * <p>三条语义要和文档保持一致：口令只进不出（BCrypt 密文落库，{@link UserView} 无 password 字段）；
 * {@code username} 冲突是 409；删除是逻辑删除（{@code deleted = 1}）。删除用户<b>不做</b>「仍持有角色」的
 * 反向引用校验，与删除角色的 409 不对称——这是有意决定：用户与角色的关系由 {@code t_user_role} 承载，
 * 用户逻辑删除后关联行保留（同删角色时保留 {@code t_role_module} 的策略），不存在悬空引用问题。
 */
@Service
public class UserService {

    /** 排序白名单：{@code PageQuery.orderBy} 直接来自客户端，必须校验后才允许进入 {@link Pageable}。 */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "username", "nickname", "status", "createTime", "updateTime");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 建号即启用；口令在此处编码，明文不落库、不出方法。 */
    @Transactional
    public UserView create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(GlobalErrorCode.CONFLICT, "用户名已存在: " + request.username());
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserEntity.STATUS_ENABLED);
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAvatar(request.avatar());
        return toView(userRepository.save(user));
    }

    /** 只改资料；用户名不可改，状态/口令/角色各有专属接口。 */
    @Transactional
    public UserView update(Long id, UserUpdateRequest request) {
        UserEntity user = requireActive(id);
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAvatar(request.avatar());
        return toView(user);
    }

    /** 启用/禁用共用：状态只有 0/1 两态，拆成两个路由没有额外信息。 */
    @Transactional
    public UserView changeStatus(Long id, UserStatusRequest request) {
        UserEntity user = requireActive(id);
        user.setStatus(request.status().byteValue());
        return toView(user);
    }

    /**
     * 逻辑删除。<b>有意</b>不做角色反向引用校验（与 {@link RoleService#delete} 的 409 不对称）：
     * 删用户不会让任何数据悬空，{@code t_user_role} 关联行随 {@code deleted} 标记一起保留，便于审计与误删恢复。
     */
    @Transactional
    public void delete(Long id) {
        UserEntity user = requireActive(id);
        userRepository.softDeleteById(user.getId());
    }

    /** 管理员重置口令：不校验旧口令；新密文覆盖 {@code password} 列。 */
    @Transactional
    public void resetPassword(Long id, UserResetPasswordRequest request) {
        UserEntity user = requireActive(id);
        user.setPassword(passwordEncoder.encode(request.password()));
    }

    /** 覆盖式分配：提交的集合整体替换 {@code t_user_role} 关联；空集合表示收回全部角色，未知 roleId 返回 400。 */
    @Transactional
    public UserView assignRoles(Long id, @Nullable Set<Long> roleIds) {
        UserEntity user = requireActive(id);
        user.setRoles(validatedRoles(roleIds));
        return toView(user);
    }

    @Transactional(readOnly = true)
    public PageResult<UserView> page(UserQuery query) {
        String orderBy = query.getOrderBy();
        if (StringUtils.hasText(orderBy) && !SORTABLE_FIELDS.contains(orderBy)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不支持的排序字段: " + orderBy);
        }
        if (!StringUtils.hasText(orderBy)) {
            // 不给排序条件时按主键兜底，保证翻页稳定
            query.setOrderBy("id");
        }
        Page<UserEntity> page = userRepository.findAll(usernameLikeAndStatus(query), query.toPageable());
        return PageResult.of(
                page, page.getContent().stream().map(UserService::toView).toList());
    }

    @Transactional(readOnly = true)
    public UserView detail(Long id) {
        return toView(requireActive(id));
    }

    private UserEntity requireActive(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在: " + id));
    }

    private Set<RoleEntity> validatedRoles(@Nullable Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        // 显式遍历判空：不可变 Set 的 contains(null) 会直接抛 NPE，走不了统一的 400
        for (Long roleId : roleIds) {
            if (roleId == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "roleId 不能为 null");
            }
        }
        List<RoleEntity> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> found = roles.stream().map(RoleEntity::getId).collect(Collectors.toSet());
            String missing = roleIds.stream()
                    .filter(rid -> !found.contains(rid))
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "角色不存在: " + missing);
        }
        return new LinkedHashSet<>(roles);
    }

    private static Specification<UserEntity> usernameLikeAndStatus(UserQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getUsername())) {
                predicates.add(builder.like(root.get("username"), likePattern(query.getUsername())));
            }
            if (query.getStatus() != null) {
                predicates.add(
                        builder.equal(root.get("status"), query.getStatus().byteValue()));
            }
            return predicates.isEmpty() ? null : builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** 转义 LIKE 通配符，防止用户输入的 {@code %} / {@code _} 扩大匹配范围。 */
    private static String likePattern(String raw) {
        String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static UserView toView(UserEntity user) {
        List<Long> roleIds =
                user.getRoles().stream().map(RoleEntity::getId).sorted().toList();
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getStatus() == null ? null : Integer.valueOf(user.getStatus()),
                roleIds,
                user.getCreator(),
                user.getCreateTime(),
                user.getUpdater(),
                user.getUpdateTime());
    }
}
