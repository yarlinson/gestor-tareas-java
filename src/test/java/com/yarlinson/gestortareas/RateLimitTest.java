package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Activa el límite con un cupo pequeño; el resto de pruebas lo tienen desactivado. */
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.capacity=3",
        "app.rate-limit.window=1m"
})
class RateLimitTest extends IntegracionBase {

    @Test
    @DisplayName("Agotado el cupo, /api/auth responde 429 con Retry-After; el resto de rutas no se limita")
    void bloqueaTrasAgotarElCupo() throws Exception {
        String credenciales = json.writeValueAsString(
                Map.of("nombreUsuario", "nadie", "password", "incorrecta123"));

        // Los 3 primeros intentos llegan al login y fallan por credenciales (401), no por límite.
        for (int intento = 1; intento <= 3; intento++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credenciales))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Demasiados intentos")));

        // Fuera de /api/auth no hay límite.
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
