package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GoogleAccountService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleAccountService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario findOrCreateVerifiedAccount(String email, String googleName) {
        String normalizedEmail = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(normalizedEmail)
            .orElseGet(() -> createAccount(normalizedEmail, googleName));
    }

    private Usuario createAccount(String email, String googleName) {
        Usuario account = new Usuario();
        account.setUsuario("google_" + UUID.randomUUID().toString().replace("-", ""));
        account.setEmail(email);
        account.setNombre(normalizeName(googleName));
        account.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
        account.setRol(Usuario.Rol.usuario);
        return usuarioRepository.save(account);
    }

    private String normalizeName(String googleName) {
        String name = googleName == null || googleName.isBlank()
            ? "Usuario de Google"
            : googleName.trim();
        return name.length() <= 100 ? name : name.substring(0, 100).trim();
    }
}
