package com.yarlinson.gestortareas.admin;

import com.yarlinson.gestortareas.usuario.Rol;
import com.yarlinson.gestortareas.usuario.Usuario;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Crea el administrador inicial a partir de {@code ADMIN_USERNAME} y {@code ADMIN_PASSWORD}.
 * <p>
 * Decisiones de seguridad:
 * <ul>
 *   <li>Nadie se hace administrador por el registro público: el rol solo se asigna aquí.</li>
 *   <li>Si el nombre ya existe no se toca esa cuenta (ni se le sube el rol ni se le cambia la
 *       clave), para que registrarse antes con ese nombre no sirva para quedarse con el rol.
 *       Por eso hay que definir las variables antes del primer arranque con registro abierto.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInicializador {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicializador.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");

    @Bean
    ApplicationRunner crearAdministrador(
            AdminProperties propiedades,
            UsuarioRepository usuarios,
            PasswordEncoder codificador) {
        return argumentos -> {
            String nombre = propiedades.username().trim();
            String clave = propiedades.password();
            if (nombre.isEmpty() || clave.isEmpty()) {
                return;
            }
            if (!NOMBRE_VALIDO.matcher(nombre).matches() || clave.length() < 8 || clave.length() > 72) {
                LOG.warn("ADMIN_USERNAME o ADMIN_PASSWORD no son válidos (nombre de 3 a 50 caracteres "
                        + "alfanuméricos, '.', '-' o '_'; clave de 8 a 72 caracteres): no se crea el administrador");
                return;
            }
            String email = nombre.toLowerCase(Locale.ROOT) + "@admin.local";
            if (usuarios.existsByNombreUsuario(nombre) || usuarios.existsByEmail(email)) {
                LOG.info("El administrador '{}' ya existe: no se modifica", nombre);
                return;
            }
            usuarios.save(new Usuario(nombre, email, codificador.encode(clave), Rol.ADMIN));
            LOG.info("Se creó el administrador '{}'", nombre);
        };
    }
}
