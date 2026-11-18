package com.refugio.nueva_vida.proyecto_de_aula;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.UsuarioRepository;
import com.refugio.nueva_vida.proyecto_de_aula.security.JwtTokenService;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
	"GOOGLE_CLIENT_ID=",
	"GOOGLE_CLIENT_SECRET="
})
@AutoConfigureMockMvc
class ProyectoDeAulaApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

		MvcResult loginPage = mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andReturn();
		String loginHtml = loginPage.getResponse().getContentAsString();
		assertTrue(loginHtml.contains("name=\"contrasena\""));
		assertFalse(loginHtml.contains("/oauth2/authorization/google"));
	}

	@Test
	@Transactional
	void unauthenticatedAdoptionRequestReturnsToDogProfileAfterLogin() throws Exception {
		String username = "redirect-test-" + UUID.randomUUID();
		Usuario user = new Usuario();
		user.setUsuario(username);
		user.setNombre("Redirect test");
		user.setEmail(username + "@example.test");
		user.setContrasena(passwordEncoder.encode("redirect-test-password"));
		usuarioRepository.saveAndFlush(user);

		MvcResult protectedPage = mockMvc.perform(get("/agendar-cita/42"))
			.andExpect(status().is3xxRedirection())
			.andReturn();

		assertEquals("/login", URI.create(
			protectedPage.getResponse().getRedirectedUrl()).getPath());
		MockHttpSession session = (MockHttpSession) protectedPage.getRequest().getSession(false);
		assertNotNull(session);
		SavedRequest savedRequest = (SavedRequest) session.getAttribute(
			"SPRING_SECURITY_SAVED_REQUEST");
		assertNotNull(savedRequest);
		assertTrue(savedRequest.getRedirectUrl().contains("/agendar-cita/42"));

		MvcResult loginPage = mockMvc.perform(get("/login").session(session))
			.andExpect(status().isOk())
			.andReturn();
		Matcher csrfToken = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
			.matcher(loginPage.getResponse().getContentAsString());
		assertTrue(csrfToken.find());

		MvcResult login = mockMvc.perform(post("/login")
				.session(session)
				.param("usuario", username)
				.param("contrasena", "redirect-test-password")
				.param("_csrf", csrfToken.group(1)))
			.andExpect(status().is3xxRedirection())
			.andReturn();
		assertEquals("/mascota/42", URI.create(
			login.getResponse().getRedirectedUrl()).getPath());
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
