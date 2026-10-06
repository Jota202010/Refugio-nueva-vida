package com.refugio.nueva_vida.proyecto_de_aula.config;

import com.refugio.nueva_vida.proyecto_de_aula.security.CustomUserDetailsService;
import com.refugio.nueva_vida.proyecto_de_aula.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
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
                .defaultSuccessUrl("/inicio", true)     // a dónde va si el login es exitoso
                .failureUrl("/login?error=true")        // a dónde va si falla
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
