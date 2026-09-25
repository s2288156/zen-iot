package com.zen.admin.controller;

import com.zen.admin.config.ZenAdminOpenApiConfiguration;
import com.zen.admin.dto.UserCreateRequest;
import com.zen.admin.dto.UserQuery;
import com.zen.admin.dto.UserResetPasswordRequest;
import com.zen.admin.dto.UserStatusRequest;
import com.zen.admin.dto.UserUpdateRequest;
import com.zen.admin.dto.UserView;
import com.zen.admin.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理。读接口登录即可（不在白名单，由 {@code AuthInterceptor} 兜底），写接口一律要求 {@code admin} 模块，
 * 与 {@code /roles/**} 同一判定链路。
 *
 * <p>响应只有 {@link UserView}，绝不回传口令。错误语义：用户名重复 409；未知 roleId、非法排序字段 400；
 * 用户不存在 404。删除为逻辑删除，且有意不做角色反向引用校验（与删角色的 409 不对称）。
 */
@Tag(name = "用户管理", description = "建号、资料维护、启停、逻辑删除、重置口令与角色分配；口令只入不出。")
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "新建用户", description = "username 全库唯一，重复返回 409；建号即启用，口令以 BCrypt 密文落库。")
    @PostMapping
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<UserView> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @Operation(summary = "修改用户资料", description = "只改昵称/邮箱/手机号/头像，传 null 视为清空；username 不可改，状态/口令/角色走各自接口。")
    @PutMapping("/{id}")
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<UserView> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @Operation(summary = "启用/禁用用户", description = "status 只接受 1（启用）/ 0（禁用）；被禁用的用户无法登录。")
    @PatchMapping("/{id}/status")
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<UserView> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.success(userService.changeStatus(id, request));
    }

    @Operation(summary = "删除用户", description = "逻辑删除（deleted=1），删除后所有查询立即不可见；有意不校验用户是否仍持有角色。")
    @DeleteMapping("/{id}")
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "重置用户口令", description = "管理员重置，不校验旧口令；新口令 8-72 字符，以 BCrypt 密文覆盖。")
    @PutMapping("/{id}/password")
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<Void> resetPassword(
            @PathVariable Long id, @Valid @RequestBody UserResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "分配用户角色", description = "覆盖式写入 t_user_role：提交的集合整体替换现有角色，空数组收回全部角色；未知 roleId 返回 400。")
    @PutMapping("/{id}/roles")
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<UserView> assignRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        return ApiResponse.success(userService.assignRoles(id, roleIds));
    }

    @Operation(
            summary = "分页查询用户",
            description = "用户名模糊匹配、状态精确匹配均可空；排序字段白名单：id/username/nickname/status/createTime/updateTime。")
    @GetMapping("/page")
    public ApiResponse<PageResult<UserView>> page(@ParameterObject @Valid UserQuery query) {
        return ApiResponse.success(userService.page(query));
    }

    @Operation(summary = "用户详情", description = "按主键查询未删除的用户，不存在返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<UserView> detail(@PathVariable Long id) {
        return ApiResponse.success(userService.detail(id));
    }
}
