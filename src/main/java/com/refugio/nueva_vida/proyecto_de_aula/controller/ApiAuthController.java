package com.refugio.nueva_vida.proyecto_de_aula.controller;

import com.refugio.nueva_vida.proyecto_de_aula.security.JwtTokenService;
import com.refugio.nueva_vida.proyecto_de_aula.web.JwtLoginRequest;
import com.refugio.nueva_vida.proyecto_de_aula.web.JwtLoginResponse;
import com.refugio.nueva_vida.proyecto_de_aula.web.JwtPrincipalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public ApiAuthController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<?> createToken(@Valid @RequestBody JwtLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    request.username().trim(), request.password()));
            return ResponseEntity.ok(new JwtLoginResponse(
                jwtTokenService.issueToken(authentication),
                "Bearer",
                jwtTokenService.getTokenLifetimeSeconds()));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "El usuario o la contraseña no son correctos."));
        }
    }

    @GetMapping("/me")
    public JwtPrincipalResponse currentPrincipal(Authentication authentication) {
        return new JwtPrincipalResponse(
            authentication.getName(),
            authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList());
    }
}
