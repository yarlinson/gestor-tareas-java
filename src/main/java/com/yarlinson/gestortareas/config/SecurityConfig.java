package com.yarlinson.gestortareas.config;

import com.yarlinson.gestortareas.ratelimit.RateLimitFilter;
import com.yarlinson.gestortareas.ratelimit.RateLimitProperties;
import com.yarlinson.gestortareas.security.JwtAuthenticationFilter;
import com.yarlinson.gestortareas.security.JwtService;
import com.yarlinson.gestortareas.security.UsuarioDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({RateLimitProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/",
            "/api/auth/**",
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain cadenaDeSeguridad(
            HttpSecurity http,
            JwtService jwtService,
            UsuarioDetailsService detallesDeUsuario,
            RateLimitProperties limiteDeIntentos) throws Exception {
        http
                // Usa el bean corsConfigurationSource. Va antes que la autenticación para que las
                // peticiones de comprobación previa (OPTIONS) del navegador no necesiten token.
                .cors(Customizer.withDefaults())
                // API sin estado que usa tokens en el encabezado (no cookies): CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Autenticado pero sin el rol necesario (@PreAuthorize): 403, sin pasar por /error.
                        .accessDeniedHandler((peticion, respuesta, ex) ->
                                respuesta.setStatus(HttpStatus.FORBIDDEN.value())))
                // El límite de intentos va primero: así un abuso se corta antes de tocar la base de datos.
                .addFilterBefore(
                        new RateLimitFilter(limiteDeIntentos),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, detallesDeUsuario),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS solo para {@code /api/**} y solo para los orígenes configurados. No se permiten
     * credenciales del navegador (cookies): la autenticación va en la cabecera Authorization.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties propiedades) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.allowedOrigins());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Sin esto el navegador oculta estas cabeceras al código JavaScript del cliente.
        configuracion.setExposedHeaders(List.of("Retry-After", "Location"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }
}
