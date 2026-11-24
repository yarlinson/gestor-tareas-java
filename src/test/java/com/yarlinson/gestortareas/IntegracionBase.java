package com.yarlinson.gestortareas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: arranca la aplicación completa (con un PostgreSQL
 * real en Docker, Flyway y la cadena de seguridad real) y ofrece utilidades comunes.
 * <p>
 * Requiere Docker en ejecución. La imagen es la misma que usa docker-compose.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracionBase {

    /**
     * Contenedor "singleton": se inicia una sola vez y lo comparten todas las clases de
     * prueba, en lugar de arrancar uno nuevo por clase. Testcontainers lo elimina al
     * terminar la JVM. {@code @ServiceConnection} apunta el DataSource de Spring hacia él.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    protected static final String CLAVE_VALIDA = "ClaveSegura123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected static String nombreUnico() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String registrarYObtenerToken(String nombreUsuario) throws Exception {
        String cuerpo = json.writeValueAsString(Map.of(
                "nombreUsuario", nombreUsuario,
                "email", nombreUsuario + "@correo.test",
                "password", CLAVE_VALIDA));
        String respuesta = mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("token").asText();
    }

    protected String cuerpoTarea(String titulo, String descripcion) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("titulo", titulo);
        cuerpo.put("descripcion", descripcion);
        return json.writeValueAsString(cuerpo);
    }

    protected long crearTarea(String token, String titulo, String descripcion) throws Exception {
        String respuesta = mvc.perform(post("/api/tareas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTarea(titulo, descripcion)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("id").asLong();
    }
}
