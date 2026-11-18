package com.refugio.nueva_vida.proyecto_de_aula.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnExpression(
    "'${GOOGLE_CLIENT_ID:}'.trim().length() > 0 or '${GOOGLE_CLIENT_SECRET:}'.trim().length() > 0"
)
public class GoogleOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository googleClientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET}") String clientSecret) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException(
                "GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET must both be configured to enable Google login.");
        }
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId(clientId.trim())
            .clientSecret(clientSecret.trim())
            .scope("openid", "profile", "email")
            .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
