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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
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
	@Transactional
	void adminCanConsultAgendaForTodayAndAnotherDate() throws Exception {
		String username = "agenda-test-" + UUID.randomUUID();
		Usuario admin = new Usuario();
		admin.setUsuario(username);
		admin.setNombre("Admin de agenda");
		admin.setEmail(username + "@example.test");
		admin.setContrasena(passwordEncoder.encode("agenda-test-password"));
		admin.setRol(Usuario.Rol.administrador);
		usuarioRepository.saveAndFlush(admin);

		MockHttpSession session = loginAs(username, "agenda-test-password");

		mockMvc.perform(get("/admin/agenda").session(session))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Agenda de hoy")))
			.andExpect(content().string(containsString("No hay citas confirmadas para este día")));

		mockMvc.perform(get("/admin/agenda")
				.session(session)
				.param("fecha", "2026-11-18"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("value=\"2026-11-18\"")))
			.andExpect(content().string(containsString("No hay citas confirmadas para este día")));
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

	@Test
	@Transactional
	void userCanUpdatePersonalDataWithoutChangingLoginOrRole() throws Exception {
		String username = "profile-test-" + UUID.randomUUID();
		Usuario user = new Usuario();
		user.setUsuario(username);
		user.setNombre("Nombre anterior");
		user.setEmail(username + "@example.test");
		user.setContrasena(passwordEncoder.encode("profile-test-password"));
		String encodedPassword = user.getContrasena();
		usuarioRepository.saveAndFlush(user);

		MockHttpSession session = loginAs(username, "profile-test-password");
		MvcResult profile = mockMvc.perform(get("/perfil").session(session))
			.andExpect(status().isOk())
			.andReturn();
		String profileHtml = profile.getResponse().getContentAsString();
		String csrfToken = extractCsrf(profileHtml);
		assertTrue(profileHtml.contains("class=\"profile-editor\""));
		assertFalse(profileHtml.contains("class=\"profile-editor\" open"));

		mockMvc.perform(post("/perfil")
				.session(session)
				.param("_csrf", csrfToken)
				.param("nombre", "  Nuevo Nombre  ")
				.param("email", "Nuevo.Email@example.test")
				.param("telefono", " 3001234567 ")
				.param("direccion", " Calle 10 "))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/perfil"));

		Usuario updated = usuarioRepository.findByUsuario(username).orElseThrow();
		assertEquals("Nuevo Nombre", updated.getNombre());
		assertEquals("nuevo.email@example.test", updated.getEmail());
		assertEquals("3001234567", updated.getTelefono());
		assertEquals("Calle 10", updated.getDireccion());
		assertEquals(username, updated.getUsuario());
		assertEquals(Usuario.Rol.usuario, updated.getRol());
		assertEquals(encodedPassword, updated.getContrasena());
	}

	@Test
	@Transactional
	void profileUpdateRejectsEmailUsedByAnotherAccount() throws Exception {
		String username = "profile-test-" + UUID.randomUUID();
		Usuario user = new Usuario();
		user.setUsuario(username);
		user.setNombre("Nombre original");
		user.setEmail(username + "@example.test");
		user.setContrasena(passwordEncoder.encode("profile-test-password"));
		usuarioRepository.saveAndFlush(user);

		Usuario otherUser = new Usuario();
		otherUser.setUsuario("other-" + UUID.randomUUID());
		otherUser.setNombre("Otra cuenta");
		otherUser.setEmail("ocupado@example.test");
		otherUser.setContrasena(passwordEncoder.encode("other-profile-password"));
		usuarioRepository.saveAndFlush(otherUser);

		MockHttpSession session = loginAs(username, "profile-test-password");
		MvcResult profile = mockMvc.perform(get("/perfil").session(session))
			.andExpect(status().isOk())
			.andReturn();
		String csrfToken = extractCsrf(profile.getResponse().getContentAsString());

		MvcResult invalidUpdate = mockMvc.perform(post("/perfil")
				.session(session)
				.param("_csrf", csrfToken)
				.param("nombre", "Nombre modificado")
				.param("email", "ocupado@example.test")
				.param("telefono", "")
				.param("direccion", ""))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("El correo electrónico ya está registrado.")))
			.andReturn();
		assertTrue(invalidUpdate.getResponse().getContentAsString()
			.contains("class=\"profile-editor\" open"));

		Usuario unchanged = usuarioRepository.findByUsuario(username).orElseThrow();
		assertEquals("Nombre original", unchanged.getNombre());
		assertEquals(username + "@example.test", unchanged.getEmail());
	}

	private MockHttpSession loginAs(String username, String password) throws Exception {
		MvcResult loginPage = mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andReturn();
		String csrfToken = extractCsrf(loginPage.getResponse().getContentAsString());
		MockHttpSession session = (MockHttpSession) loginPage.getRequest().getSession(false);
		assertNotNull(session);

		MvcResult login = mockMvc.perform(post("/login")
				.session(session)
				.param("usuario", username)
				.param("contrasena", password)
				.param("_csrf", csrfToken))
			.andExpect(status().is3xxRedirection())
			.andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	private String extractCsrf(String html) {
		Matcher csrfToken = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
		assertTrue(csrfToken.find());
		return csrfToken.group(1);
	}
}
