package com.zen.common.core.jwt;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

/** HS256 密钥推导。密钥不足 32 字节时 {@code Keys.hmacShaKeyFor} 抛 {@code WeakKeyException},让启动直接失败。 */
final class JwtKeys {

  private JwtKeys() {}

  static SecretKey hmacKey(JwtProperties properties) {
    String secret = properties.getSecret();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("zen.jwt.secret 未配置,无法签发或校验 Token");
    }
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
