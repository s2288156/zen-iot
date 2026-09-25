package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新建用户入参。{@code username} 全库唯一，创建后不可改；口令以 BCrypt 密文落库，任何接口都不会回传。
 * 资料字段（昵称/邮箱/手机号/头像）均可空，后续用 {@code PUT /users/{id}} 维护。
 */
public record UserCreateRequest(
        @NotBlank @Size(max = 64) @Schema(description = "用户名；全库唯一，重复返回 409，创建后不可改", example = "wcs-operator")
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(
                description = "初始口令，8-72 字符（BCrypt 以 72 字节为上限），只入不出",
                format = "password",
                example = "replace-with-your-dev-password")
        String password,

        @Size(max = 64) @Schema(description = "昵称，可空", example = "张三")
        String nickname,

        @Email @Size(max = 128) @Schema(description = "邮箱，可空；仅格式校验，不保证唯一", example = "zhangsan@example.com")
        String email,

        @Size(max = 32) @Schema(description = "手机号，可空；不做格式强校验，长度兜底", example = "13800000000")
        String phone,

        @Size(max = 255) @Schema(description = "头像 URL，可空", example = "https://cdn.example.com/avatar/1.png")
        String avatar) {}
