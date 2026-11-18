package com.refugio.nueva_vida.proyecto_de_aula.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GmailApiEmailSenderTests {

    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    private GmailCredentialService credentialService;
    private GmailApiEmailSender sender;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        credentialService = mock(GmailCredentialService.class);
        when(credentialService.requireMailbox()).thenReturn(
            new GmailCredentialService.GmailMailbox("refugio@gmail.com", "refresh-token"));
        sender = new GmailApiEmailSender(builder, credentialService, CLIENT_ID, CLIENT_SECRET, false);
    }

    @Test
    void renuevaElTokenYEnviaElMensajeMimeCodificadoAlaApiDeGmail() {
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", containsString("application/x-www-form-urlencoded")))
            .andExpect(content().string(containsString("grant_type=refresh_token")))
            .andExpect(content().string(containsString("refresh_token=refresh-token")))
            .andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andExpect(jsonPath("$.raw").value(not(isEmptyOrNullString())))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        sender.send("", "adoptante@example.com", "Aviso", "Texto del correo", "<p>Texto</p>");

        server.verify();
    }

    @Test
    void informaSiGoogleRechazaLaRenovacionDelTokenSinIntentarEnviar() {
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(MailSendException.class,
            () -> sender.send("refugio@gmail.com", "adoptante@example.com", "Aviso", "Texto", "<p>Texto</p>"));

        server.verify();
    }
}
