package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** 新建角色入参。{@code roleCode} 全库唯一，创建后不可改；模块授权后续走 {@code PUT /roles/{id}/modules} 覆盖式调整。 */
public record RoleCreateRequest(
        @NotBlank @Size(max = 64) @Schema(description = "角色编码；全库唯一，重复返回 409", example = "wcs-operator")
        String roleCode,

        @NotBlank @Size(max = 64) @Schema(description = "角色名称，展示用", example = "WCS 操作员")
        String roleName,

        @Size(max = 255) @Schema(description = "描述，可空", example = "负责 WCS 任务与站点分配")
        String description,

        @Schema(description = "授予的模块集合，取值限 admin/wcs/rcs/ecs，非法值 400；空或缺省表示不授予任何模块", example = "[\"wcs\"]")
        Set<String> modules) {

    public RoleCreateRequest {
        modules = modules == null ? Set.of() : Set.copyOf(modules);
    }
}
