package com.zen.admin.controller;

import com.zen.admin.config.ZenAdminOpenApiConfiguration;
import com.zen.admin.dto.RoleCreateRequest;
import com.zen.admin.dto.RoleQuery;
import com.zen.admin.dto.RoleUpdateRequest;
import com.zen.admin.dto.RoleView;
import com.zen.admin.interceptor.OperationAction;
import com.zen.admin.interceptor.OperationLog;
import com.zen.admin.interceptor.OperationTargetType;
import com.zen.admin.service.RoleService;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理。读接口登录即可（不在白名单，由 {@code AuthInterceptor} 兜底），写接口一律要求 {@code admin} 模块，
 * 与 {@code /demo/**} 同一判定链路。
 *
 * <p>响应只有 {@link RoleView}，实体不出 service 层。错误语义：{@code roleCode} 重复与删除被引用角色都是 409，
 * 非法模块编码与非法排序字段是 400，角色不存在是 404。
 */
@Tag(name = "角色管理", description = "角色的增删改查与模块授权；删除为逻辑删除，仍被用户引用的角色删不掉。")
@RestController
@RequestMapping("/roles")
@SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "新建角色", description = "roleCode 全库唯一，重复返回 409；modules 取值限 admin/wcs/rcs/ecs，非法值 400。")
    @PostMapping
    @RequireModule(ModuleCode.ADMIN)
    @OperationLog(action = OperationAction.CREATE, targetType = OperationTargetType.ROLE)
    public ApiResponse<RoleView> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    @Operation(summary = "修改角色", description = "只改名称与描述；roleCode 不可改，模块授权走 PUT /roles/{id}/modules。")
    @PutMapping("/{id}")
    @RequireModule(ModuleCode.ADMIN)
    @OperationLog(action = OperationAction.UPDATE, targetType = OperationTargetType.ROLE)
    public ApiResponse<RoleView> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @Operation(summary = "删除角色", description = "逻辑删除（deleted=1），删除后所有查询立即不可见；仍被有效用户引用返回 409。")
    @DeleteMapping("/{id}")
    @RequireModule(ModuleCode.ADMIN)
    @OperationLog(action = OperationAction.DELETE, targetType = OperationTargetType.ROLE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询角色", description = "编码/名称模糊匹配可空；排序字段白名单：id/roleCode/roleName/createTime/updateTime。")
    @GetMapping("/page")
    public ApiResponse<PageResult<RoleView>> page(@ParameterObject @Valid RoleQuery query) {
        return ApiResponse.success(roleService.page(query));
    }

    @Operation(summary = "角色详情", description = "按主键查询未删除的角色，不存在返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<RoleView> detail(@PathVariable Long id) {
        return ApiResponse.success(roleService.detail(id));
    }

    @Operation(summary = "配置角色模块权限", description = "覆盖式写入 t_role_module：提交的集合整体替换现有授权，空数组收回全部模块；非法模块编码 400。")
    @PutMapping("/{id}/modules")
    @RequireModule(ModuleCode.ADMIN)
    @OperationLog(action = OperationAction.ASSIGN_MODULES, targetType = OperationTargetType.ROLE)
    public ApiResponse<RoleView> assignModules(@PathVariable Long id, @RequestBody Set<String> modules) {
        return ApiResponse.success(roleService.assignModules(id, modules));
    }
}
