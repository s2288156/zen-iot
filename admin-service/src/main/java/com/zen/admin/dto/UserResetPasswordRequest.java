package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员重置用户口令入参。不校验旧口令——发起方已被 {@code admin} 模块鉴权兜底。 */
public record UserResetPasswordRequest(
        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(
                description = "新口令，8-72 字符（BCrypt 以 72 字节为上限），只入不出",
                format = "password",
                example = "replace-with-your-dev-password")
        String password) {}
