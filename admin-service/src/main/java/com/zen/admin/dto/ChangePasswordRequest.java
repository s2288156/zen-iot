package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 自助改密入参。旧口令只做 BCrypt 比对、不设长度下限——历史短口令用户不应被校验规则挡在改密入口外；
 * 新口令口径与 {@link UserCreateRequest}/{@link UserResetPasswordRequest} 一致。
 */
public record ChangePasswordRequest(
        @NotBlank
        @Schema(
                description = "当前口令明文，服务端只做 BCrypt 比对、不回显；文档匿名可读，故此处不放真实开发口令",
                format = "password",
                example = "replace-with-your-dev-password")
        String oldPassword,

        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(
                description = "新口令，8-72 字符（BCrypt 以 72 字节为上限），只入不出",
                format = "password",
                example = "replace-with-your-dev-password")
        String newPassword) {}
