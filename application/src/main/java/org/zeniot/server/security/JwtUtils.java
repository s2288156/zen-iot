package org.zeniot.server.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class JwtUtils {
    @Value("${secret.key}")
    private String key;

    public String newToken(UserDetails userDetails) {
        return generateToken(generateClaims(userDetails));
    }

    public boolean verifyToken(String token) {
        try {
            JWSVerifier verifier = new MACVerifier(key.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.verify(verifier);
        } catch (Exception e) {
            log.error("Token verify error: ", e);
        }
        return false;
    }

    private String generateToken(JWTClaimsSet claims) {
        // 512-bit (64-byte) shared secret
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        // Create HMAC signer
        byte[] sharedSecret = key.getBytes();
        JWSSigner signer = null;
        try {
            signer = new MACSigner(sharedSecret);
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
        claims.put(JWTClaimNames.EXPIRATION_TIME, System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
        claims.put("username", userDetails.getUsername());
        try {
            return JWTClaimsSet.parse(claims);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
