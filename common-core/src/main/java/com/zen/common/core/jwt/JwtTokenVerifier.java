package com.zen.common.core.jwt;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.List;
import javax.crypto.SecretKey;

/**
 * 校验 Token 的签名、过期时间与 {@code typ}。
 *
 * <p>任何不通过都抛 {@code BusinessException(UNAUTHORIZED)},由全局异常处理器（Servlet 侧）或网关过滤器（Reactive 侧）统一透出
 * 401,本类不关心传输层语义。
 */
public class JwtTokenVerifier {

    private final SecretKey key;

    public JwtTokenVerifier(JwtProperties properties) {
        this.key = JwtKeys.hmacKey(properties);
    }

    /**
     * @param expectedType 期望用途;Token 的 {@code typ} 不符即视为无效（如拿 refresh 访问业务接口）
     * @throws BusinessException 签名无效、已过期、格式错误或用途不符
     */
    public VerifiedToken verify(String token, TokenType expectedType) {
        Claims claims = parse(token);
        TokenType type = TokenType.fromClaimValue(claims.get(JwtClaimNames.TYPE, String.class))
                .filter(expectedType::equals)
                .orElseThrow(JwtTokenVerifier::unauthorized);
        return new VerifiedToken(
                claims.getId(),
                userId(claims),
                claims.get(JwtClaimNames.USERNAME, String.class),
                stringList(claims, JwtClaimNames.ROLES),
                stringList(claims, JwtClaimNames.MODULES),
                type,
                claims.getExpiration().toInstant());
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    private long userId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw unauthorized();
        }
    }

    private static List<String> stringList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> items) {
            return items.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static BusinessException unauthorized() {
        return new BusinessException(GlobalErrorCode.UNAUTHORIZED);
    }
}
