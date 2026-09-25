package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 修改用户资料入参。只改可空展示字段：{@code username} 是业务标识不可改，{@code status}/{@code password}/角色
 * 分别由 {@code PATCH /users/{id}/status}、{@code PUT /users/{id}/password}、{@code PUT /users/{id}/roles} 单独维护。
 */
public record UserUpdateRequest(
        @Size(max = 64) @Schema(description = "昵称；传 null 视为清空", example = "张三")
        String nickname,

        @Email @Size(max = 128) @Schema(description = "邮箱；传 null 视为清空", example = "zhangsan@example.com")
        String email,

        @Size(max = 32) @Schema(description = "手机号；传 null 视为清空", example = "13800000000")
        String phone,

        @Size(max = 255) @Schema(description = "头像 URL；传 null 视为清空", example = "https://cdn.example.com/avatar/1.png")
        String avatar) {}
