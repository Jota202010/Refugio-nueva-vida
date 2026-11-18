package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.GmailCredential;
import com.refugio.nueva_vida.proyecto_de_aula.repository.GmailCredentialRepository;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class GmailCredentialService {

    private static final int CREDENTIAL_ID = 1;

    private final GmailCredentialRepository repository;
    private final GmailTokenEncryptionService encryptionService;

    public GmailCredentialService(GmailCredentialRepository repository,
                                  GmailTokenEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    public void connect(String email, String refreshToken) {
        if (email == null || email.isBlank() || refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Google no entregó una cuenta y autorización válidas.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 150) {
            throw new IllegalArgumentException("El correo de Gmail no es válido.");
        }
        repository.save(new GmailCredential(
            CREDENTIAL_ID, normalizedEmail, encryptionService.encrypt(refreshToken)));
    }

    @Transactional(readOnly = true)
    public Optional<String> connectedEmail() {
        return repository.findById(CREDENTIAL_ID).map(GmailCredential::getEmail);
    }

    @Transactional(readOnly = true)
    public GmailMailbox requireMailbox() {
        GmailCredential credential = repository.findById(CREDENTIAL_ID)
            .orElseThrow(() -> new MailSendException(
                "Conecta una cuenta de Gmail desde la configuración de correo del administrador."));
        try {
            return new GmailMailbox(
                credential.getEmail(), encryptionService.decrypt(credential.getEncryptedRefreshToken()));
        } catch (IllegalStateException exception) {
            throw new MailSendException("No se pudo leer la autorización guardada de Gmail.", exception);
        }
    }

    public void disconnect() {
        repository.deleteById(CREDENTIAL_ID);
    }

    public record GmailMailbox(String email, String refreshToken) {
    }
}
