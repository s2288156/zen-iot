package com.zen.common.core.jwt;

/** JWT 自定义 claim 名,签发与解析两侧共用同一份字面量。 */
final class JwtClaimNames {

  static final String USERNAME = "username";
  static final String TYPE = "typ";
  static final String ROLES = "roles";
  static final String MODULES = "modules";

  private JwtClaimNames() {}
}
