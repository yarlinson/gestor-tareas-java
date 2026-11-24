package com.yarlinson.gestortareas.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Administrador que se crea al arrancar (propiedades {@code app.admin.*}). Si alguno de los dos
 * valores está vacío no se crea ninguno.
 *
 * @param username nombre del administrador
 * @param password clave inicial del administrador (8 a 72 caracteres); se guarda con BCrypt
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @DefaultValue("") String username,
        @DefaultValue("") String password) {
}
