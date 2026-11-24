package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Detrás de un proxy, el límite debe contar por la IP de la cabecera configurada, no por la de la conexión. */
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.capacity=3",
        "app.rate-limit.window=1m",
        "app.rate-limit.client-ip-header=CF-Connecting-IP"
})
class RateLimitClientIpTest extends IntegracionBase {

    private ResultActions intentarLogin(String ipDelCliente) throws Exception {
        var peticion = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombreUsuario", "nadie", "password", "incorrecta123")));
        if (ipDelCliente != null) {
            peticion.header("CF-Connecting-IP", ipDelCliente);
        }
        return mvc.perform(peticion);
    }

    @Test
    @DisplayName("Cada IP de la cabecera tiene su propio cupo, aunque la conexión sea la misma")
    void cuentaPorLaIpDeLaCabecera() throws Exception {
        for (int intento = 1; intento <= 3; intento++) {
            intentarLogin("203.0.113.10").andExpect(status().isUnauthorized());
        }
        intentarLogin("203.0.113.10").andExpect(status().isTooManyRequests());

        // Otro cliente detrás del mismo proxy no se ve afectado.
        intentarLogin("203.0.113.20").andExpect(status().isUnauthorized());
        // Las IPv6 también se aceptan.
        intentarLogin("2001:db8::1").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una cabecera sin formato de IP se ignora y se usa la IP de la conexión")
    void ignoraValoresQueNoSonIp() throws Exception {
        // Si el valor se aceptara como clave, cada texto distinto tendría un cupo nuevo y se evadiría el límite.
        for (int intento = 1; intento <= 3; intento++) {
            intentarLogin("no-soy-una-ip-" + intento).andExpect(status().isUnauthorized());
        }
        intentarLogin("otra-cosa-distinta").andExpect(status().isTooManyRequests());
    }
}
