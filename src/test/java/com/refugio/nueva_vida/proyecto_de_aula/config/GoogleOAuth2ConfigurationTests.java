package com.refugio.nueva_vida.proyecto_de_aula.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "GOOGLE_CLIENT_ID=test-client-id",
    "GOOGLE_CLIENT_SECRET=test-client-secret"
})
@AutoConfigureMockMvc
class GoogleOAuth2ConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void configuredClientEnablesGoogleLoginAndAuthorizationRedirect() throws Exception {
        assertNotNull(clientRegistrationRepository);
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Continuar con Google")))
            .andExpect(content().string(containsString("/oauth2/authorization/google")));

        mockMvc.perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location",
                startsWith("https://accounts.google.com/o/oauth2/v2/auth?")));
    }
}
