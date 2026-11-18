package com.refugio.nueva_vida.proyecto_de_aula.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GmailTokenEncryptionServiceTests {

    @Test
    void cifraYDescifraTokensConUnaClaveDeClienteOAuthEstable() {
        GmailTokenEncryptionService encryptionService =
            new GmailTokenEncryptionService("independent-encryption-secret");

        String encrypted = encryptionService.encrypt("refresh-token-value");

        assertNotEquals("refresh-token-value", encrypted);
        assertEquals("refresh-token-value", encryptionService.decrypt(encrypted));
    }

    @Test
    void noPermiteDescifrarConOtroSecretoOAuth() {
        String encrypted = new GmailTokenEncryptionService("first-secret").encrypt("refresh-token-value");
        GmailTokenEncryptionService rotatedSecret = new GmailTokenEncryptionService("second-secret");

        assertThrows(IllegalStateException.class, () -> rotatedSecret.decrypt(encrypted));
    }
}
