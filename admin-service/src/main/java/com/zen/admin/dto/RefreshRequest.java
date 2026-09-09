package com.zen.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** 用 refresh Token 换新双 Token。 */
public record RefreshRequest(@NotBlank String refreshToken) {}
