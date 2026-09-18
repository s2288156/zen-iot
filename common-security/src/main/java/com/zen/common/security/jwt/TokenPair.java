package com.zen.common.security.jwt;

/** 一次登录签发的双 Token。 */
public record TokenPair(String accessToken, String refreshToken) {}
