package com.refugio.nueva_vida.proyecto_de_aula.security;

import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.service.GoogleAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final GoogleAccountService googleAccountService;
    private final CustomUserDetailsService userDetailsService;

    @Autowired
    public GoogleOidcUserService(GoogleAccountService googleAccountService,
                                 CustomUserDetailsService userDetailsService) {
        this(new org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService(),
            googleAccountService, userDetailsService);
    }

    GoogleOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate,
                          GoogleAccountService googleAccountService,
                          CustomUserDetailsService userDetailsService) {
        this.delegate = delegate;
        this.googleAccountService = googleAccountService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws AuthenticationException {
        OidcUser googleUser = delegate.loadUser(userRequest);
        String email = googleUser.getAttribute("email");
        Boolean emailVerified = googleUser.getAttribute("email_verified");
        if (!Boolean.TRUE.equals(emailVerified) || email == null || email.isBlank() || email.length() > 150) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                "invalid_google_account",
                "Google debe proporcionar un correo verificado y compatible.",
                null
            ));
        }

        String name = googleUser.getAttribute("name");
        Usuario account = googleAccountService.findOrCreateVerifiedAccount(email, name);
        UserDetails accountDetails = userDetailsService.loadUserByUsername(account.getUsuario());
        return new GooglePrincipal(googleUser, accountDetails);
    }
}
