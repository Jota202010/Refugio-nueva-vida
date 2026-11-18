package com.refugio.nueva_vida.proyecto_de_aula.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class GmailTokenEncryptionService {

    private static final String KEY_DERIVATION_CONTEXT = "Refugio Nueva Vida Gmail refresh token v1:";
    private static final int NONCE_LENGTH = 12;

    private final String encryptionSecret;
    private final SecureRandom secureRandom = new SecureRandom();

    public GmailTokenEncryptionService(
            @Value("${app.mail.gmail.token-encryption-key:}") String encryptionSecret) {
        this.encryptionSecret = encryptionSecret;
    }

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce)
                + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo proteger la autorización de Gmail.", exception);
        }
    }

    public String decrypt(String value) {
        String[] parts = value.split(":", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            throw new IllegalStateException("El formato de autorización de Gmail no es válido.");
        }
        try {
            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("No se pudo leer la autorización guardada de Gmail.", exception);
        }
    }

    private SecretKeySpec encryptionKey() throws GeneralSecurityException {
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            throw new IllegalStateException("GMAIL_TOKEN_ENCRYPTION_KEY debe estar configurado.");
        }
        byte[] key = MessageDigest.getInstance("SHA-256").digest(
            (KEY_DERIVATION_CONTEXT + encryptionSecret).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
