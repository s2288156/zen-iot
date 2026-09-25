package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改角色入参。只改展示属性：{@code roleCode} 是业务标识、不出现在请求里，模块授权由
 * {@code PUT /roles/{id}/modules} 单独覆盖。
 */
public record RoleUpdateRequest(
        @NotBlank @Size(max = 64) @Schema(description = "角色名称，展示用", example = "WCS 高级操作员")
        String roleName,

        @Size(max = 255) @Schema(description = "描述，可空；传 null 视为清空", example = "含 WCS 配置维护权限")
        String description) {}
