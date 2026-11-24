package com.yarlinson.gestortareas.auth;

import com.yarlinson.gestortareas.common.ConflictoException;
import com.yarlinson.gestortareas.security.JwtService;
import com.yarlinson.gestortareas.usuario.Usuario;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final AuthenticationManager gestorDeAutenticacion;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarios,
            PasswordEncoder codificador,
            AuthenticationManager gestorDeAutenticacion,
            JwtService jwtService) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.gestorDeAutenticacion = gestorDeAutenticacion;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse registrar(RegistroRequest solicitud) {
        String nombreUsuario = solicitud.nombreUsuario().trim();
        String email = solicitud.email().trim().toLowerCase(Locale.ROOT);

        if (usuarios.existsByNombreUsuario(nombreUsuario)) {
            throw new ConflictoException("El nombre de usuario ya está en uso");
        }
        if (usuarios.existsByEmail(email)) {
            throw new ConflictoException("El correo ya está registrado");
        }

        Usuario usuario = usuarios.save(
                new Usuario(nombreUsuario, email, codificador.encode(solicitud.password())));
        return emitirToken(usuario.getNombreUsuario());
    }

    public TokenResponse iniciarSesion(LoginRequest solicitud) {
        String nombreUsuario = solicitud.nombreUsuario().trim();
        // Lanza AuthenticationException (-> 401) si las credenciales no son correctas.
        gestorDeAutenticacion.authenticate(
                new UsernamePasswordAuthenticationToken(nombreUsuario, solicitud.password()));
        return emitirToken(nombreUsuario);
    }

    private TokenResponse emitirToken(String nombreUsuario) {
        return TokenResponse.bearer(jwtService.generar(nombreUsuario), jwtService.segundosDeVigencia());
    }
}
