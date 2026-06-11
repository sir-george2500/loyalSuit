package com.loyalsuit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates the application's own JWTs (HS256). The platform is the
 * identity provider — no external auth service is involved.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    /** Short-lived window for completing a 2FA challenge after the password step. */
    private static final long MFA_CHALLENGE_SECONDS = 300;
    private static final String MFA_PURPOSE = "mfa";

    public String issueToken(UUID userId, String email, String role, UUID tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("tenantId", tenantId != null ? tenantId.toString() : null)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Issues a short-lived token that proves the password step passed but 2FA is still pending.
     * It carries a {@code purpose=mfa} claim so it can never be used as a bearer token.
     */
    public String issueMfaChallenge(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", MFA_PURPOSE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(MFA_CHALLENGE_SECONDS)))
                .signWith(signingKey)
                .compact();
    }

    /** Validates an MFA challenge token and returns the user it was issued for. */
    public UUID parseMfaChallenge(String token) {
        Claims claims = parse(token);
        if (!MFA_PURPOSE.equals(claims.get("purpose", String.class))) {
            throw new IllegalArgumentException("Not an MFA challenge token");
        }
        return UUID.fromString(claims.getSubject());
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal toPrincipal(Claims claims) {
        // An MFA challenge token is not a credential — refuse to build a principal from one.
        if (MFA_PURPOSE.equals(claims.get("purpose", String.class))) {
            throw new IllegalArgumentException("MFA challenge token cannot authenticate a request");
        }
        String tenantIdRaw = claims.get("tenantId", String.class);
        return new UserPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class),
                tenantIdRaw != null ? UUID.fromString(tenantIdRaw) : null
        );
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public Map<String, Object> describeToken(Claims claims) {
        return Map.of(
                "userId", claims.getSubject(),
                "email", claims.get("email", String.class),
                "role", claims.get("role", String.class)
        );
    }
}
