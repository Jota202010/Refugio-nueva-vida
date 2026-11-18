package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

@Service
public class GmailApiEmailSender {

    private static final Logger logger = LoggerFactory.getLogger(GmailApiEmailSender.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final RestClient restClient;
    private final GmailCredentialService credentialService;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public GmailApiEmailSender(RestClient.Builder builder,
                               GmailCredentialService credentialService,
                               @Value("${app.mail.gmail.client-id:}") String clientId,
                               @Value("${app.mail.gmail.client-secret:}") String clientSecret) {
        this(builder, credentialService, clientId, clientSecret, true);
    }

    GmailApiEmailSender(RestClient.Builder builder,
                        GmailCredentialService credentialService,
                        String clientId,
                        String clientSecret,
                        boolean configureTimeouts) {
        if (configureTimeouts) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(5));
            requestFactory.setReadTimeout(Duration.ofSeconds(15));
            this.restClient = builder.clone().requestFactory(requestFactory).build();
        } else {
            this.restClient = builder.clone().build();
        }
        this.credentialService = credentialService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void send(String from, String to, String subject, String text, String html) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new MailSendException("Falta configurar GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET.");
        }
        GmailCredentialService.GmailMailbox mailbox = credentialService.requireMailbox();
        String accessToken = refreshAccessToken(mailbox.refreshToken());
        String remitente = from == null || from.isBlank() ? mailbox.email() : from;
        String rawMessage = encodeMessage(remitente, to, subject, text, html);

        try {
            restClient.post()
                .uri(SEND_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GmailSendRequest(rawMessage))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            logger.warn("Gmail API rechazó un correo (HTTP {}).", status);
            throw new MailSendException("Gmail API rechazó el correo (HTTP " + status + ").", exception);
        } catch (RestClientException exception) {
            logger.warn("No se pudo conectar con Gmail API ({}).",
                exception.getClass().getSimpleName());
            throw new MailSendException("No se pudo conectar con Gmail API.", exception);
        }
    }

    private String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        try {
            GoogleTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new MailSendException("Google no devolvió un token de acceso válido.");
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            logger.warn("Google rechazó la renovación de autorización de Gmail (HTTP {}).", status);
            throw new MailSendException(
                "La autorización de Gmail venció o fue revocada. Vuelve a conectar la cuenta desde administración.",
                exception);
        } catch (RestClientException exception) {
            logger.warn("No se pudo renovar la autorización de Gmail ({}).",
                exception.getClass().getSimpleName());
            throw new MailSendException("No se pudo renovar la autorización de Gmail.", exception);
        }
    }

    private String encodeMessage(String from, String to, String subject, String text, String html) {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            message.saveChanges();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            message.writeTo(output);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
        } catch (MessagingException | IOException exception) {
            throw new MailPreparationException("No se pudo preparar el correo para Gmail API.", exception);
        }
    }

    private record GmailSendRequest(String raw) {
    }

    private record GoogleTokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}
