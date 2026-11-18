package com.refugio.nueva_vida.proyecto_de_aula.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "GOOGLE_CLIENT_ID=test-client-id",
    "GOOGLE_CLIENT_SECRET=test-client-secret"
})
class GoogleOAuth2ConfigurationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void configuredClientEnablesGoogleLoginAndAuthorizationRedirect() {
        assertNotNull(clientRegistrationRepository);
        ResponseEntity<String> loginResponse = restTemplate.getForEntity("/login", String.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("Continuar con Google"));
        assertTrue(loginResponse.getBody().contains("/oauth2/authorization/google"));

        ResponseEntity<Void> authorizationResponse =
            restTemplate.getForEntity("/oauth2/authorization/google", Void.class);
        assertEquals(HttpStatus.FOUND, authorizationResponse.getStatusCode());
        assertTrue(authorizationResponse.getHeaders().getLocation().toString()
            .startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
    }

    @Test
    void forwardedHttpsHeadersAreUsedForGoogleCallbackUri() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-Proto", "https");
        headers.set("X-Forwarded-Host", "refugio-app-production-d56d2768.up.railway.app");
        ResponseEntity<Void> response = restTemplate.exchange(
            "/oauth2/authorization/google",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Void.class
        );
        assertEquals(HttpStatus.FOUND, response.getStatusCode());

        String location = response.getHeaders().getLocation().toString();
        assertEquals(
            "https://refugio-app-production-d56d2768.up.railway.app/login/oauth2/code/google",
            UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("redirect_uri")
        );
    }

    @Test
    void gmailSendPermissionIsLimitedToSeparateAdminConnection() {
        var loginRegistration = clientRegistrationRepository.findByRegistrationId("google");
        var gmailRegistration = clientRegistrationRepository.findByRegistrationId("google-mail");

        assertTrue(loginRegistration.getScopes().contains("email"));
        assertTrue(!loginRegistration.getScopes().contains("https://www.googleapis.com/auth/gmail.send"));
        assertTrue(gmailRegistration.getScopes().contains("https://www.googleapis.com/auth/gmail.send"));
        assertTrue(gmailRegistration.getProviderDetails().getAuthorizationUri().contains("prompt=consent"));
        assertTrue(gmailRegistration.getProviderDetails().getAuthorizationUri().contains("access_type=offline"));
    }
}
