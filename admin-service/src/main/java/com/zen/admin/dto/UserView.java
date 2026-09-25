package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图。绝对不含 {@code password}；角色只回 {@code roleIds}（升序），前端需要角色名时自行对照
 * {@code GET /roles/page}，避免列表接口为展示角色名引入嵌套查询。
 */
public record UserView(
        @Schema(description = "用户ID", example = "3") Long id,

        @Schema(description = "用户名", example = "wcs-operator")
        String username,

        @Schema(description = "昵称") String nickname,
        @Schema(description = "邮箱") String email,
        @Schema(description = "手机号") String phone,
        @Schema(description = "头像 URL") String avatar,

        @Schema(description = "状态：1 启用 / 0 禁用", example = "1")
        Integer status,

        @Schema(description = "已分配角色ID，按升序", example = "[1,2]")
        List<Long> roleIds,

        @Schema(description = "创建人用户名") String creator,
        @Schema(description = "创建时间") LocalDateTime createTime,
        @Schema(description = "最后更新人用户名") String updater,
        @Schema(description = "最后更新时间") LocalDateTime updateTime) {

    public UserView {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
