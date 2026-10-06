package com.refugio.nueva_vida.proyecto_de_aula.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTests {

    private static final String SECRET =
        Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void issuesAndParsesShortLivedTokensWithTheAuthenticatedRole() {
        JwtTokenService service = new JwtTokenService(SECRET);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            "adoptante1", null, List.of(new SimpleGrantedAuthority("ROLE_usuario")));

        String token = service.issueToken(authentication);
        JwtTokenService.ParsedToken parsed = service.parseToken(token);

        assertEquals("adoptante1", parsed.subject());
        assertEquals(List.of("ROLE_usuario"), parsed.roles());
        assertEquals(900, service.getTokenLifetimeSeconds());
    }

    @Test
    void rejectsTokensSignedWithAnotherKeyAndTamperedTokens() {
        JwtTokenService issuer = new JwtTokenService(SECRET);
        JwtTokenService otherKey = new JwtTokenService(
            Base64.getEncoder().encodeToString("abcdef0123456789abcdef0123456789".getBytes()));
        String token = issuer.issueToken(UsernamePasswordAuthenticationToken.authenticated(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_administrador"))));

        assertThrows(JwtException.class, () -> otherKey.parseToken(token));
        assertThrows(JwtException.class, () -> issuer.parseToken(token + "tampered"));
    }

    @Test
    void rejectsSigningKeysShorterThan256Bits() {
        String weakSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThrows(IllegalStateException.class, () -> new JwtTokenService(weakSecret));
    }

    @Test
    void rejectsExpiredTokens() {
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        Instant expiredAt = Instant.now().minusSeconds(60);
        String expiredToken = Jwts.builder()
            .issuer("refugio-nueva-vida")
            .subject("adoptante1")
            .claim("roles", List.of("ROLE_usuario"))
            .issuedAt(Date.from(expiredAt.minusSeconds(60)))
            .expiration(Date.from(expiredAt))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertThrows(JwtException.class, () -> new JwtTokenService(SECRET).parseToken(expiredToken));
    }
}
