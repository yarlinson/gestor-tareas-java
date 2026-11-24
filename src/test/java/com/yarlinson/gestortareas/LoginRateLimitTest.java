package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El límite por usuario debe funcionar aunque el cliente cambie de IP en cada intento. */
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.capacity=1000",
        "app.rate-limit.login-capacity-per-user=3",
        "app.rate-limit.window=1m"
})
class LoginRateLimitTest extends IntegracionBase {

    private ResultActions login(String nombreUsuario, String clave, String ipFalsa) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", ipFalsa)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombreUsuario", nombreUsuario, "password", clave))));
    }

    @Test
    @DisplayName("Tras agotar los intentos de un usuario responde 429, aunque cambie la IP")
    void limitaPorUsuarioSinImportarLaIp() throws Exception {
        String usuario = nombreUnico();

        for (int intento = 1; intento <= 3; intento++) {
            login(usuario, "incorrecta123", "198.51.100." + intento).andExpect(status().isUnauthorized());
        }

        login(usuario, "incorrecta123", "198.51.100.99")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value(containsString("este usuario")));
    }

    @Test
    @DisplayName("El cupo es independiente para cada usuario")
    void otroUsuarioNoSeVeAfectado() throws Exception {
        String atacado = nombreUnico();
        String otro = nombreUnico();
        registrarYObtenerToken(otro);

        for (int intento = 1; intento <= 4; intento++) {
            login(atacado, "incorrecta123", "203.0.113.5");
        }

        login(otro, CLAVE_VALIDA, "203.0.113.5").andExpect(status().isOk());
    }
}
