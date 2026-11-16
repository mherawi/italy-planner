package com.italyplanner;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Component
class AccountTokens {
    static final Duration LIFETIME = Duration.ofHours(8);
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    AccountTokens(@Value("${planner.jwt.secret:}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.isBlank()) {
            key = new byte[32];
            new SecureRandom().nextBytes(key);
        } else if (key.length < 32) {
            throw new IllegalArgumentException("PLANNER_JWT_SECRET must be at least 32 bytes");
        }
        var signingKey = new SecretKeySpec(key, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(signingKey));
        var verifier = NimbusJwtDecoder.withSecretKey(signingKey).macAlgorithm(MacAlgorithm.HS256).build();
        verifier.setJwtValidator(JwtValidators.createDefaultWithIssuer("italy-planner"));
        decoder = verifier;
    }

    String issue(String email) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("italy-planner").subject(email)
                .issuedAt(now).expiresAt(now.plus(LIFETIME)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    String email(String token) {
        Jwt jwt = decoder.decode(token);
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now()) ||
                jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new BadJwtException("Invalid account claims");
        }
        return jwt.getSubject();
    }
}
