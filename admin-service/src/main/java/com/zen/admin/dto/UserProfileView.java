package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 当前登录用户的个人资料视图。绝对不含 {@code password}；形状对齐 {@link UserView} 但去掉审计四列
 * （creator/createTime/updater/updateTime 是管理视角，不属于个人中心），也不含 modules——前端菜单授权不在本接口范围。
 */
public record UserProfileView(
        @Schema(description = "用户ID", example = "1") Long id,

        @Schema(description = "用户名", example = "admin") String username,

        @Schema(description = "昵称") String nickname,
        @Schema(description = "邮箱") String email,
        @Schema(description = "手机号") String phone,
        @Schema(description = "头像 URL") String avatar,

        @Schema(description = "状态：1 启用 / 0 禁用", example = "1")
        Integer status,

        @Schema(description = "已分配角色ID，按升序", example = "[1]")
        List<Long> roleIds) {

    public UserProfileView {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
