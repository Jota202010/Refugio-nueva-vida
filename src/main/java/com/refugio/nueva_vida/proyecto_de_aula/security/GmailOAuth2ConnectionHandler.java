package com.refugio.nueva_vida.proyecto_de_aula.security;

import com.refugio.nueva_vida.proyecto_de_aula.service.GmailCredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GmailOAuth2ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailOAuth2ConnectionHandler.class);
    public static final String ADMIN_AUTHENTICATION_SESSION_KEY =
        GmailOAuth2ConnectionHandler.class.getName() + ".adminAuthentication";
    private static final String REGISTRATION_ID = "google-mail";

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final SecurityContextRepository securityContextRepository;
    private final GmailCredentialService gmailCredentialService;

    public GmailOAuth2ConnectionHandler(OAuth2AuthorizedClientRepository authorizedClientRepository,
                                        SecurityContextRepository securityContextRepository,
                                        GmailCredentialService gmailCredentialService) {
        this.authorizedClientRepository = authorizedClientRepository;
        this.securityContextRepository = securityContextRepository;
        this.gmailCredentialService = gmailCredentialService;
    }

    public void beginConnection(HttpServletRequest request, Authentication authentication) {
        if (authentication == null
                || !authentication.getAuthorities().contains(
                    new SimpleGrantedAuthority("ROLE_administrador"))) {
            throw new IllegalStateException("Solo un administrador puede conectar Gmail.");
        }
        request.getSession(true).setAttribute(ADMIN_AUTHENTICATION_SESSION_KEY, authentication);
    }

    public boolean handleSuccess(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)
                || !REGISTRATION_ID.equals(oauthToken.getAuthorizedClientRegistrationId())) {
            return false;
        }

        Authentication adminAuthentication = getAdminAuthentication(request);
        if (adminAuthentication == null) {
            removeAuthorizedClient(request, response, authentication);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return true;
        }

        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
            REGISTRATION_ID, authentication, request);
        OAuth2RefreshToken refreshToken = client == null ? null : client.getRefreshToken();
        if (refreshToken == null || !(authentication.getPrincipal() instanceof OidcUser user)
                || user.getEmail() == null || user.getEmail().isBlank()) {
            removeAuthorizedClient(request, response, authentication);
            restoreAdminAuthentication(request, response, adminAuthentication);
            response.sendRedirect(request.getContextPath() + "/admin/correo?error=authorization");
            return true;
        }

        try {
            gmailCredentialService.connect(user.getEmail(), refreshToken.getTokenValue());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.warn("No se pudo guardar la autorización de Gmail ({}).",
                exception.getClass().getSimpleName());
            removeAuthorizedClient(request, response, authentication);
            restoreAdminAuthentication(request, response, adminAuthentication);
            response.sendRedirect(request.getContextPath() + "/admin/correo?error=save");
            return true;
        }

        removeAuthorizedClient(request, response, authentication);
        restoreAdminAuthentication(request, response, adminAuthentication);
        response.sendRedirect(request.getContextPath() + "/admin/correo?connected=true");
        return true;
    }

    public boolean handleFailure(HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        Authentication adminAuthentication = getAdminAuthentication(request);
        if (adminAuthentication == null) {
            return false;
        }
        restoreAdminAuthentication(request, response, adminAuthentication);
        response.sendRedirect(request.getContextPath() + "/admin/correo?error=authorization");
        return true;
    }

    private Authentication getAdminAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ADMIN_AUTHENTICATION_SESSION_KEY);
        if (!(value instanceof Authentication authentication)
                || !authentication.getAuthorities().contains(
                    new SimpleGrantedAuthority("ROLE_administrador"))) {
            return null;
        }
        return authentication;
    }

    private void restoreAdminAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(ADMIN_AUTHENTICATION_SESSION_KEY);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void removeAuthorizedClient(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        authorizedClientRepository.removeAuthorizedClient(
            REGISTRATION_ID, authentication, request, response);
    }
}
