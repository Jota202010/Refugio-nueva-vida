package com.refugio.nueva_vida.proyecto_de_aula;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.refugio.nueva_vida.proyecto_de_aula.security.JwtTokenService;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProyectoDeAulaApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Test
	void contextLoads() {
	}

	@Test
	void apiProfileRequiresValidBearerToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void validJwtAuthenticatesApiWithoutChangingWebLogin() throws Exception {
		String token = jwtTokenService.issueToken(
			UsernamePasswordAuthenticationToken.authenticated(
				"adoptante1", null, List.of(new SimpleGrantedAuthority("ROLE_usuario"))));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("adoptante1"))
			.andExpect(jsonPath("$.roles[0]").value("ROLE_usuario"));

		mockMvc.perform(get("/login"))
			.andExpect(status().isOk());
	}

	@Test
	void publicSupportChatStatusRemainsAccessible() throws Exception {
		mockMvc.perform(get("/api/support-chat/status"))
			.andExpect(status().isOk());
	}

	@Test
	void apiTokenEndpointDoesNotRequireCsrfAndRejectsUnknownCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/token")
				.contentType("application/json")
				.content("""
					{"username":"missing-user","password":"not-a-password"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").exists());
	}
}
