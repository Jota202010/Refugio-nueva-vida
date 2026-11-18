package com.refugio.nueva_vida.proyecto_de_aula.security;

import com.refugio.nueva_vida.proyecto_de_aula.service.GmailCredentialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GmailOAuth2ConnectionHandlerTests {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void guardaLaAutorizacionYRestauraLaSesionDelAdministrador() throws Exception {
        OAuth2AuthorizedClientRepository authorizedClientRepository =
            mock(OAuth2AuthorizedClientRepository.class);
        SecurityContextRepository securityContextRepository = mock(SecurityContextRepository.class);
        GmailCredentialService credentialService = mock(GmailCredentialService.class);
        GmailOAuth2ConnectionHandler handler = new GmailOAuth2ConnectionHandler(
            authorizedClientRepository, securityContextRepository, credentialService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication admin = new TestingAuthenticationToken(
            "admin", "credentials", "ROLE_administrador");
        handler.beginConnection(request, admin);

        OAuth2AuthenticationToken googleAuthentication = mock(OAuth2AuthenticationToken.class);
        OidcUser googleUser = mock(OidcUser.class);
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh-token", Instant.now());
        when(googleAuthentication.getAuthorizedClientRegistrationId()).thenReturn("google-mail");
        when(googleAuthentication.getPrincipal()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("Refugio@example.com");
        when(authorizedClientRepository.loadAuthorizedClient(
            eq("google-mail"), eq(googleAuthentication), eq(request))).thenReturn(authorizedClient);
        when(authorizedClient.getRefreshToken()).thenReturn(refreshToken);

        assertTrue(handler.handleSuccess(request, response, googleAuthentication));

        verify(credentialService).connect("Refugio@example.com", "refresh-token");
        verify(authorizedClientRepository).removeAuthorizedClient(
            "google-mail", googleAuthentication, request, response);
        verify(securityContextRepository).saveContext(
            org.mockito.ArgumentMatchers.any(), eq(request), eq(response));
        assertEquals("/admin/correo?connected=true", response.getRedirectedUrl());
        assertEquals(admin, org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication());
        assertNull(request.getSession(false).getAttribute(
            GmailOAuth2ConnectionHandler.ADMIN_AUTHENTICATION_SESSION_KEY));
    }

    @Test
    void noInterceptaElInicioDeSesionGoogleNormal() throws Exception {
        GmailOAuth2ConnectionHandler handler = new GmailOAuth2ConnectionHandler(
            mock(OAuth2AuthorizedClientRepository.class),
            mock(SecurityContextRepository.class),
            mock(GmailCredentialService.class));

        assertFalse(handler.handleSuccess(
            new MockHttpServletRequest(), new MockHttpServletResponse(),
            mock(OAuth2AuthenticationToken.class)));
    }
}
