package com.yarlinson.gestortareas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Orígenes del navegador autorizados a llamar a la API (propiedades {@code app.cors.*}).
 * <p>
 * Son orígenes exactos (por ejemplo {@code https://mi-web.vercel.app}), sin comodines: con un
 * comodín cualquier página podría leer respuestas de la API en nombre de un usuario con sesión.
 * Si la lista está vacía no se autoriza ningún origen, es decir, CORS queda desactivado.
 *
 * @param allowedOrigins lista de orígenes; se ignoran los vacíos y las barras finales
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origen -> !origen.isEmpty())
                        .map(origen -> origen.endsWith("/") ? origen.substring(0, origen.length() - 1) : origen)
                        .toList();
    }
}
