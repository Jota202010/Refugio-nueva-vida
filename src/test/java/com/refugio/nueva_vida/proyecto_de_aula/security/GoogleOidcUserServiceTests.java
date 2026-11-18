package com.refugio.nueva_vida.proyecto_de_aula.security;

import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.service.GoogleAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GoogleOidcUserServiceTests {

    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private GoogleAccountService googleAccountService;
    private CustomUserDetailsService userDetailsService;
    private OidcUserRequest request;
    private OidcUser googleUser;

    @BeforeEach
    void setUp() {
        delegate = mock(OAuth2UserService.class);
        googleAccountService = mock(GoogleAccountService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        request = mock(OidcUserRequest.class);
        googleUser = mock(OidcUser.class);
        when(delegate.loadUser(request)).thenReturn(googleUser);
    }

    @Test
    void verifiedGoogleEmailCreatesOrFindsAccountAndReceivesItsDatabaseRole() {
        Usuario account = new Usuario();
        account.setUsuario("adoptante");
        when(googleUser.<String>getAttribute("email")).thenReturn("adoptante@example.com");
        when(googleUser.<Boolean>getAttribute("email_verified")).thenReturn(true);
        when(googleUser.<String>getAttribute("name")).thenReturn("Adoptante");
        when(googleUser.getIdToken()).thenReturn(new OidcIdToken(
            "id-token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("sub", "google-sub", "email", "adoptante@example.com")
        ));
        when(googleUser.getUserInfo()).thenReturn(new OidcUserInfo(Map.of(
            "sub", "google-sub", "email", "adoptante@example.com", "name", "Adoptante")));
        when(googleAccountService.findOrCreateVerifiedAccount(
            "adoptante@example.com", "Adoptante")).thenReturn(account);
        when(userDetailsService.loadUserByUsername("adoptante")).thenReturn(
            User.withUsername("adoptante").password("encoded").authorities(
                new SimpleGrantedAuthority("ROLE_usuario")).build());

        OidcUser result = new GoogleOidcUserService(
            delegate, googleAccountService, userDetailsService).loadUser(request);

        GooglePrincipal principal = assertInstanceOf(GooglePrincipal.class, result);
        assertEquals("adoptante", principal.getUsername());
        assertEquals("", principal.getPassword());
        assertEquals("ROLE_usuario", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void rejectsUnverifiedGoogleEmailBeforeCreatingAnAccount() {
        when(googleUser.<String>getAttribute("email")).thenReturn("adoptante@example.com");
        when(googleUser.<Boolean>getAttribute("email_verified")).thenReturn(false);

        assertThrows(OAuth2AuthenticationException.class, () ->
            new GoogleOidcUserService(delegate, googleAccountService, userDetailsService).loadUser(request));

        verifyNoInteractions(googleAccountService, userDetailsService);
    }
}
