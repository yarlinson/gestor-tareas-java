package com.yarlinson.gestortareas;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.admin.username=admin_pruebas",
        "app.admin.password=ClaveAdmin1234"
})
class AdminControllerTest extends IntegracionBase {

    private static final String AUTH = "Authorization";

    private String tokenDeAdmin() throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("nombreUsuario", "admin_pruebas", "password", "ClaveAdmin1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("token").asText();
    }

    private JsonNode leer(String ruta, String token) throws Exception {
        String respuesta = mvc.perform(get(ruta).header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    @Test
    @DisplayName("Un usuario corriente recibe 403 y sin token 401 en el panel de administración")
    void usuarioCorrienteNoEntra() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        mvc.perform(get("/api/admin/estadisticas").header(AUTH, bearer(token))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios").header(AUTH, bearer(token))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/estadisticas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El administrador definido por variables de entorno puede iniciar sesión y entrar")
    void administradorEntra() throws Exception {
        String token = tokenDeAdmin();

        mvc.perform(get("/api/admin/estadisticas").header(AUTH, bearer(token))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/usuarios").header(AUTH, bearer(token))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("El nombre del administrador no se puede registrar de forma pública")
    void nombreDeAdministradorReservado() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nombreUsuario", "admin_pruebas",
                                "email", "otro@correo.test",
                                "password", CLAVE_VALIDA))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("El listado muestra el rol y el número de tareas de cada usuario, sin claves")
    void listaUsuariosConSusTareas() throws Exception {
        String nombre = nombreUnico();
        String token = registrarYObtenerToken(nombre);
        crearTarea(token, "Una", null);
        crearTarea(token, "Dos", null);

        JsonNode pagina = leer("/api/admin/usuarios?size=100", tokenDeAdmin());

        JsonNode encontrado = null;
        for (JsonNode usuario : pagina.get("contenido")) {
            if (nombre.equals(usuario.get("nombreUsuario").asText())) {
                encontrado = usuario;
            }
        }
        assertThat(encontrado).as("el usuario recién creado aparece en el listado").isNotNull();
        assertThat(encontrado.get("rol").asText()).isEqualTo("USER");
        assertThat(encontrado.get("totalTareas").asLong()).isEqualTo(2);
        assertThat(encontrado.has("password")).isFalse();
    }

    @Test
    @DisplayName("Las estadísticas reflejan los usuarios y las tareas creadas")
    void estadisticasCuentanTareas() throws Exception {
        String tokenAdmin = tokenDeAdmin();
        JsonNode antes = leer("/api/admin/estadisticas", tokenAdmin);

        String token = registrarYObtenerToken(nombreUnico());
        long completada = crearTarea(token, "Hecha", null);
        crearTarea(token, "Pendiente", null);
        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Atrasada\",\"fechaLimite\":\"2000-01-01\"}"))
                .andExpect(status().isCreated());
        mvc.perform(patch("/api/tareas/{id}/estado", completada)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completada\":true}"))
                .andExpect(status().isOk());

        JsonNode despues = leer("/api/admin/estadisticas", tokenAdmin);

        assertThat(despues.get("totalUsuarios").asLong() - antes.get("totalUsuarios").asLong()).isEqualTo(1);
        assertThat(despues.get("totalTareas").asLong() - antes.get("totalTareas").asLong()).isEqualTo(3);
        assertThat(despues.get("tareasCompletadas").asLong() - antes.get("tareasCompletadas").asLong()).isEqualTo(1);
        assertThat(despues.get("tareasPendientes").asLong() - antes.get("tareasPendientes").asLong()).isEqualTo(2);
        assertThat(despues.get("tareasVencidas").asLong() - antes.get("tareasVencidas").asLong()).isEqualTo(1);
    }
}
