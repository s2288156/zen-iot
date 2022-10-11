package org.zeniot.server.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Component
public class JwtHandler {
    @Value("${secret.key}")
    private String key;
    @Value("${secret.expiration}")
    private Long expiration;

    private static final String USERNAME = "username";
    private static final String AUTHORITY = "authority";

    public String newToken(UserDetails userDetails) {
        return generateToken(generateClaims(userDetails));
    }

    public boolean verifyToken(String token, UserDetails userDetails) {
        try {
            JWSVerifier verifier = new MACVerifier(key.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            String username = (String) signedJWT.getJWTClaimsSet().getClaim("username");
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return signedJWT.verify(verifier)
                    && StringUtils.equals(username, userDetails.getUsername())
                    && expirationTime.after(new Date());
        } catch (Exception e) {
            log.error("Token verify error: ", e);
        }
        return false;
    }

    public String getUsernameForToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim(USERNAME);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserForToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String username = signedJWT.getJWTClaimsSet().getStringClaim(USERNAME);
            //noinspection unchecked
            Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) signedJWT.getJWTClaimsSet().getClaim(AUTHORITY);
            return new User(username, "", authorities);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateToken(JWTClaimsSet claims) {
        // 512-bit (64-byte) shared secret
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        try {
            JWSSigner signer = new MACSigner(key.getBytes());
            // Apply the HMAC protection
            signedJWT.sign(signer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Serialize to compact form, produces something like
        return signedJWT.serialize();
    }

    private JWTClaimsSet generateClaims(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWTClaimNames.EXPIRATION_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8")) + expiration);
        claims.put(USERNAME, userDetails.getUsername());
        claims.put(AUTHORITY, userDetails.getAuthorities());
        try {
            return JWTClaimsSet.parse(claims);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
