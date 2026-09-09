package com.zen.common.core.jwt;

import java.util.Arrays;
import java.util.Optional;

/** Token 用途,落在 {@code typ} claim 上;用于阻止 refresh token 被当作 access token 访问业务接口。 */
public enum TokenType {
  ACCESS("access"),
  REFRESH("refresh");

  private final String claimValue;

  TokenType(String claimValue) {
    this.claimValue = claimValue;
  }

  public String claimValue() {
    return claimValue;
  }

  public static Optional<TokenType> fromClaimValue(String value) {
    return Arrays.stream(values()).filter(type -> type.claimValue.equals(value)).findFirst();
  }
}
