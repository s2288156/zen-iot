package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 角色视图。审计字段来自 {@code BaseEntity}，无敏感内容；{@code modules} 已排序，保证文档与响应稳定。 */
public record RoleView(
        @Schema(description = "角色ID", example = "3") Long id,

        @Schema(description = "角色编码", example = "wcs-operator")
        String roleCode,

        @Schema(description = "角色名称", example = "WCS 操作员") String roleName,
        @Schema(description = "描述") String description,

        @Schema(description = "已授予的模块，按字典序", example = "[\"wcs\"]")
        List<String> modules,

        @Schema(description = "创建人用户名") String creator,
        @Schema(description = "创建时间") LocalDateTime createTime,
        @Schema(description = "最后更新人用户名") String updater,
        @Schema(description = "最后更新时间") LocalDateTime updateTime) {

    public RoleView {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
