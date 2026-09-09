package com.zen.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** 账号密码登录入参。 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
