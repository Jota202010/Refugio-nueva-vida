package com.refugio.nueva_vida.proyecto_de_aula.controller;

import com.refugio.nueva_vida.proyecto_de_aula.security.JwtTokenService;
import com.refugio.nueva_vida.proyecto_de_aula.web.JwtLoginRequest;
import com.refugio.nueva_vida.proyecto_de_aula.web.JwtLoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiAuthControllerTests {

    @Test
    void authenticatesCredentialsAndReturnsBearerTokenMetadata() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        var principal = User.withUsername("adoptante1")
            .password("encoded-password")
            .authorities(new SimpleGrantedAuthority("ROLE_usuario"))
            .build();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenService.issueToken(authentication)).thenReturn("signed-jwt");
        when(jwtTokenService.getTokenLifetimeSeconds()).thenReturn(900L);

        ApiAuthController controller = new ApiAuthController(authenticationManager, jwtTokenService);
        var response = controller.createToken(new JwtLoginRequest("adoptante1", "password"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JwtLoginResponse body = assertInstanceOf(JwtLoginResponse.class, response.getBody());
        assertEquals("signed-jwt", body.accessToken());
        assertEquals("Bearer", body.tokenType());
        assertEquals(900, body.expiresIn());
        verify(jwtTokenService).issueToken(authentication);
    }

    @Test
    void rejectsInvalidCredentialsWithoutReturningAToken() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("invalid credentials"));

        ApiAuthController controller = new ApiAuthController(authenticationManager, jwtTokenService);
        var response = controller.createToken(new JwtLoginRequest("unknown", "incorrect"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(jwtTokenService, org.mockito.Mockito.never()).issueToken(any());
    }
}
