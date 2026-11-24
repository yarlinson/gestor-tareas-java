package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilControllerTest extends IntegracionBase {

    @Test
    @DisplayName("Devuelve el usuario autenticado con su rol y sin la clave")
    void devuelveElPerfil() throws Exception {
        String nombre = nombreUnico();
        String token = registrarYObtenerToken(nombre);

        mvc.perform(get("/api/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value(nombre))
                .andExpect(jsonPath("$.email").value(nombre + "@correo.test"))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Sin token responde 401")
    void exigeToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}
