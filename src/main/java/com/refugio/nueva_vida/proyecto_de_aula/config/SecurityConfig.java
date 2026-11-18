package com.refugio.nueva_vida.proyecto_de_aula.config;

import com.refugio.nueva_vida.proyecto_de_aula.security.CustomUserDetailsService;
import com.refugio.nueva_vida.proyecto_de_aula.security.GmailOAuth2ConnectionHandler;
import com.refugio.nueva_vida.proyecto_de_aula.security.GoogleOidcUserService;
import com.refugio.nueva_vida.proyecto_de_aula.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Pattern ADOPTION_FORM_PATH = Pattern.compile("^/agendar-cita/(\\d+)/?$");
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler(RequestCache requestCache) {
        SavedRequestAwareAuthenticationSuccessHandler defaultHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        defaultHandler.setRequestCache(requestCache);

        return (request, response, authentication) -> {
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                String requestedPath = URI.create(savedRequest.getRedirectUrl()).getPath();
                String contextPath = request.getContextPath();
                if (!contextPath.isEmpty() && requestedPath.startsWith(contextPath)) {
                    requestedPath = requestedPath.substring(contextPath.length());
                }

                Matcher adoptionPath = ADOPTION_FORM_PATH.matcher(requestedPath);
                if (adoptionPath.matches()) {
                    requestCache.removeRequest(request, response);
                    response.sendRedirect(contextPath + "/mascota/" + adoptionPath.group(1));
                    return;
                }
            }
            defaultHandler.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/token"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Se requiere un token Bearer válido.\"}");
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/token", "/api/support-chat/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Qualifier("loginSuccessHandler")
                                               AuthenticationSuccessHandler loginSuccessHandler,
                                           RequestCache requestCache,
                                           ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                           ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> googleOidcUserService,
                                           OAuth2AuthorizedClientRepository authorizedClientRepository,
                                           SecurityContextRepository securityContextRepository,
                                           GmailOAuth2ConnectionHandler gmailConnectionHandler)
            throws Exception {
        http
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .requestCache(cache -> cache.requestCache(requestCache))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas: cualquiera puede acceder
                .requestMatchers("/", "/inicio", "/nosotros", "/mision",
                                 "/login", "/registro", "/css/**", "/js/**",
                                 "/images/**", "/fotos/**", "/mascota/**",
                                 "/error", "/error/**").permitAll()
                // Rutas de admin: solo rol administrador
                .requestMatchers("/admin/**").hasRole("administrador")
                // Rutas de usuario autenticado
                .requestMatchers("/perfil", "/agendar-cita/**").hasAnyRole("usuario", "administrador")
                // Todo lo demás requiere login
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")                    // página de login personalizada
                .loginProcessingUrl("/login")           // Spring Security intercepta este POST
                .usernameParameter("usuario")           // nombre del campo en el form
                .passwordParameter("contrasena")        // nombre del campo password en el form
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error=true")        // a dónde va si falla
                .permitAll()
            );

        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .authorizedClientRepository(authorizedClientRepository)
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService.getObject()))
                .successHandler((request, response, authentication) -> {
                    if (!gmailConnectionHandler.handleSuccess(request, response, authentication)) {
                        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
                    }
                })
                .failureHandler((request, response, exception) -> {
                    if (gmailConnectionHandler.handleFailure(request, response)) {
                        return;
                    }
                    String oauthErrorCode = exception instanceof OAuth2AuthenticationException oauthException
                        ? oauthException.getError().getErrorCode()
                        : "not_available";
                    Throwable cause = exception.getCause();
                    String causeType = cause == null ? "none" : cause.getClass().getSimpleName();
                    String causeOAuthErrorCode = cause instanceof OAuth2AuthorizationException authorizationException
                        ? authorizationException.getError().getErrorCode()
                        : "not_available";
                    LOGGER.warn("Google OAuth login failed: exceptionType={}, oauthErrorCode={}, "
                            + "causeType={}, causeOAuthErrorCode={}",
                        exception.getClass().getSimpleName(), oauthErrorCode, causeType, causeOAuthErrorCode);
                    response.sendRedirect(request.getContextPath() + "/login?oauth2Error=true");
                })
                .permitAll()
            );
        }

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        return http.build();
    }
}
