package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAccountServiceTests {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private GoogleAccountService googleAccountService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        googleAccountService = new GoogleAccountService(usuarioRepository, passwordEncoder);
        when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
    }

    @Test
    void createsVerifiedGoogleAccountWithUserRoleAndUnusableRandomPassword() {
        when(usuarioRepository.findByEmail("nuevo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario account = googleAccountService.findOrCreateVerifiedAccount(
            " Nuevo@Example.com ", "Nuevo Adoptante");

        assertEquals("nuevo@example.com", account.getEmail());
        assertEquals("Nuevo Adoptante", account.getNombre());
        assertTrue(account.getUsuario().matches("google_[0-9a-f]{32}"));
        assertEquals("encoded-random-password", account.getContrasena());
        assertEquals(Usuario.Rol.usuario, account.getRol());
        verify(usuarioRepository).save(account);
    }

    @Test
    void reusesExistingAccountWithoutChangingItsRoleOrPassword() {
        Usuario existing = new Usuario();
        existing.setEmail("admin@example.com");
        existing.setUsuario("admin");
        existing.setRol(Usuario.Rol.administrador);
        existing.setContrasena("existing-password-hash");
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        Usuario result = googleAccountService.findOrCreateVerifiedAccount(
            "ADMIN@example.com", "Google Profile Name");

        assertEquals(existing, result);
        assertEquals(Usuario.Rol.administrador, result.getRol());
        assertEquals("existing-password-hash", result.getContrasena());
        assertNotEquals("Google Profile Name", result.getNombre());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }
}
