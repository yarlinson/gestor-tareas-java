package com.yarlinson.gestortareas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee el encabezado {@code Authorization: Bearer <token>} y, si el token es válido,
 * autentica la petición. Si no lo es, deja la petición sin autenticar y será la
 * cadena de seguridad la que responda 401.
 * <p>
 * No se anota como {@code @Component} a propósito: así Spring Boot no lo registra
 * una segunda vez como filtro de servlet fuera de la cadena de seguridad.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService detallesDeUsuario;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService detallesDeUsuario) {
        this.jwtService = jwtService;
        this.detallesDeUsuario = detallesDeUsuario;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain cadena) throws ServletException, IOException {

        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean sinAutenticar = SecurityContextHolder.getContext().getAuthentication() == null;

        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER) && sinAutenticar) {
            String token = encabezado.substring(PREFIJO_BEARER.length()).trim();
            jwtService.extraerNombreUsuario(token).ifPresent(nombre -> autenticar(nombre, request));
        }
        cadena.doFilter(request, response);
    }

    private void autenticar(String nombreUsuario, HttpServletRequest request) {
        try {
            UserDetails usuario = detallesDeUsuario.loadUserByUsername(nombreUsuario);
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (UsernameNotFoundException ex) {
            // El usuario del token ya no existe: la petición queda sin autenticar.
        }
    }
}
