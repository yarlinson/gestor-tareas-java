package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000, https://mi-web.vercel.app/")
class CorsTest extends IntegracionBase {

    @Test
    @DisplayName("La comprobación previa de un origen autorizado se responde sin token")
    void permiteElOrigenAutorizado() throws Exception {
        mvc.perform(options("/api/tareas")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization")));
    }

    @Test
    @DisplayName("Ignora los espacios y la barra final al configurar los orígenes")
    void normalizaLosOrigenes() throws Exception {
        mvc.perform(options("/api/tareas")
                        .header("Origin", "https://mi-web.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://mi-web.vercel.app"));
    }

    @Test
    @DisplayName("Rechaza los orígenes que no están en la lista")
    void rechazaOtrosOrigenes() throws Exception {
        mvc.perform(options("/api/tareas")
                        .header("Origin", "https://sitio-malicioso.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Aunque la respuesta sea un error 401, lleva la cabecera CORS y no permite credenciales")
    void respuestaRealConCabecerasCors() throws Exception {
        mvc.perform(get("/actuator/health").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tareas").header("Origin", "http://localhost:3000"))
                // Sin token es 401, pero la cabecera CORS debe estar para que el navegador deje leerlo.
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
