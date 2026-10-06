package com.refugio.nueva_vida.proyecto_de_aula.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class JwtTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenService.class);
    private static final String ISSUER = "refugio-nueva-vida";
    private static final String ROLES_CLAIM = "roles";
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_usuario", "ROLE_administrador");

    private final SecretKey signingKey;

    public JwtTokenService(@Value("${app.jwt.secret:}") String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            signingKey = Jwts.SIG.HS256.key().build();
            LOGGER.warn("JWT_SECRET is not configured; JWT tokens will become invalid after an app restart.");
            return;
        }

        byte[] decodedSecret;
        try {
            decodedSecret = Decoders.BASE64.decode(configuredSecret.trim());
        } catch (io.jsonwebtoken.io.DecodingException exception) {
            throw new IllegalStateException(
                "JWT_SECRET must be a valid Base64-encoded key of at least 256 bits.", exception);
        }
        try {
            signingKey = Keys.hmacShaKeyFor(decodedSecret);
        } catch (WeakKeyException exception) {
            throw new IllegalStateException(
                "JWT_SECRET must be a Base64-encoded key of at least 256 bits.", exception);
        }
    }

    public String issueToken(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .sorted()
            .toList();

        if (roles.isEmpty() || !ALLOWED_ROLES.containsAll(roles)) {
            throw new IllegalArgumentException("The authenticated user has no supported role.");
        }

        Instant issuedAt = Instant.now();
        return Jwts.builder()
            .issuer(ISSUER)
            .subject(authentication.getName())
            .claim(ROLES_CLAIM, roles)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plus(TOKEN_LIFETIME)))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public ParsedToken parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(ISSUER)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        String subject = claims.getSubject();
        Object rolesClaim = claims.get(ROLES_CLAIM);
        if (subject == null || subject.isBlank() || !(rolesClaim instanceof Collection<?> roles)) {
            throw new JwtException("The token is missing required claims.");
        }

        List<String> validatedRoles = new ArrayList<>();
        for (Object role : roles) {
            if (!(role instanceof String roleName) || !ALLOWED_ROLES.contains(roleName)) {
                throw new JwtException("The token contains an unsupported role.");
            }
            validatedRoles.add(roleName);
        }
        if (validatedRoles.isEmpty()) {
            throw new JwtException("The token does not contain any roles.");
        }

        return new ParsedToken(subject, List.copyOf(validatedRoles));
    }

    public long getTokenLifetimeSeconds() {
        return TOKEN_LIFETIME.toSeconds();
    }

    public record ParsedToken(String subject, List<String> roles) {}
}
