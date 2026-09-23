package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 账号密码登录入参。 */
public record LoginRequest(
        @NotBlank @Schema(description = "用户名；种子数据里有 admin（全部模块）与 demo（只有业务模块）两个账号", example = "admin")
        String username,

        @NotBlank
        @Schema(
                description = "口令明文，服务端只做 BCrypt 比对、不回显；文档匿名可读，故此处不放真实开发口令",
                format = "password",
                example = "replace-with-your-dev-password")
        String password) {}
