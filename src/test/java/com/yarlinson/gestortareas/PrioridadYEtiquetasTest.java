package com.yarlinson.gestortareas;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrioridadYEtiquetasTest extends IntegracionBase {

    private static final String AUTH = "Authorization";

    private JsonNode crear(String token, String titulo, Map<String, Object> extras) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("titulo", titulo);
        cuerpo.putAll(extras);
        String respuesta = mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    private JsonNode crear(String token, String titulo) throws Exception {
        return crear(token, titulo, Map.of());
    }

    @Test
    @DisplayName("La prioridad por defecto es MEDIA y se puede elegir otra")
    void prioridadPorDefecto() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        assertThat(crear(token, "Sin prioridad").get("prioridad").asText()).isEqualTo("MEDIA");
        assertThat(crear(token, "Urgente", Map.of("prioridad", "ALTA")).get("prioridad").asText()).isEqualTo("ALTA");

        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Rara\",\"prioridad\":\"URGENTISIMA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Filtra por prioridad y ordena por importancia, no alfabéticamente")
    void filtraYOrdenaPorPrioridad() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        crear(token, "baja", Map.of("prioridad", "BAJA"));
        crear(token, "alta", Map.of("prioridad", "ALTA"));
        crear(token, "media", Map.of("prioridad", "MEDIA"));

        mvc.perform(get("/api/tareas").param("prioridad", "ALTA").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("alta"));

        mvc.perform(get("/api/tareas").param("sort", "prioridad,desc").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.contenido[0].prioridad").value("ALTA"))
                .andExpect(jsonPath("$.contenido[1].prioridad").value("MEDIA"))
                .andExpect(jsonPath("$.contenido[2].prioridad").value("BAJA"));

        mvc.perform(get("/api/tareas").param("prioridad", "NOEXISTE").header(AUTH, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Las etiquetas se normalizan, no se duplican y se reutilizan entre tareas")
    void etiquetasSeNormalizanYSeReutilizan() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        JsonNode primera = crear(token, "Informe", Map.of("etiquetas", List.of("Trabajo", "  URGENTE ", "trabajo")));
        assertThat(primera.get("etiquetas")).hasSize(2);
        assertThat(primera.get("etiquetas").get(0).asText()).isEqualTo("trabajo");
        assertThat(primera.get("etiquetas").get(1).asText()).isEqualTo("urgente");

        crear(token, "Reunión", Map.of("etiquetas", List.of("trabajo")));

        mvc.perform(get("/api/etiquetas").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("trabajo"))
                .andExpect(jsonPath("$[0].totalTareas").value(2))
                .andExpect(jsonPath("$[1].nombre").value("urgente"))
                .andExpect(jsonPath("$[1].totalTareas").value(1));
    }

    @Test
    @DisplayName("Filtra tareas por etiqueta sin distinguir mayúsculas")
    void filtraPorEtiqueta() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        crear(token, "Con etiqueta", Map.of("etiquetas", List.of("casa")));
        crear(token, "Sin etiqueta");

        mvc.perform(get("/api/tareas").param("etiqueta", "CASA").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Con etiqueta"));

        mvc.perform(get("/api/tareas").param("etiqueta", "otra").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Las etiquetas son de cada usuario: no se ven ni se borran entre usuarios")
    void etiquetasSonPorUsuario() throws Exception {
        String tokenA = registrarYObtenerToken(nombreUnico());
        String tokenB = registrarYObtenerToken(nombreUnico());
        crear(tokenA, "De A", Map.of("etiquetas", List.of("privada")));
        crear(tokenB, "De B", Map.of("etiquetas", List.of("privada")));

        long idDeA = json.readTree(mvc.perform(get("/api/etiquetas").header(AUTH, bearer(tokenA)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).get(0).get("id").asLong();

        mvc.perform(get("/api/etiquetas").header(AUTH, bearer(tokenB)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].totalTareas").value(1));

        mvc.perform(delete("/api/etiquetas/{id}", idDeA).header(AUTH, bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Eliminar una etiqueta la quita de las tareas que la tenían")
    void eliminarEtiquetaLaQuitaDeLasTareas() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        JsonNode tarea = crear(token, "Tarea", Map.of("etiquetas", List.of("a-borrar", "se-queda")));

        long id = json.readTree(mvc.perform(get("/api/etiquetas").header(AUTH, bearer(token)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).get(0).get("id").asLong();

        mvc.perform(delete("/api/etiquetas/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/tareas/{id}", tarea.get("id").asLong()).header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.etiquetas.length()").value(1))
                .andExpect(jsonPath("$.etiquetas[0]").value("se-queda"));
    }

    @Test
    @DisplayName("Rechaza demasiadas etiquetas y etiquetas vacías")
    void validaEtiquetas() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        List<String> once = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("titulo", "x", "etiquetas", once))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.etiquetas").exists());

        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("titulo", "x", "etiquetas", List.of("   ")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Marca como vencidas las tareas con fecha pasada y sin completar, y permite filtrarlas")
    void tareasVencidas() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        JsonNode antigua = crear(token, "Antigua", Map.of("fechaLimite", "2000-01-01"));
        crear(token, "Lejana", Map.of("fechaLimite", "2999-12-31"));
        crear(token, "Sin fecha");

        assertThat(antigua.get("vencida").asBoolean()).isTrue();

        mvc.perform(get("/api/tareas").param("vencidas", "true").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Antigua"));

        mvc.perform(get("/api/tareas").param("vencidas", "false").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(2));

        // Al completarla deja de estar vencida.
        mvc.perform(patch("/api/tareas/{id}/estado", antigua.get("id").asLong())
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completada\":true}"))
                .andExpect(jsonPath("$.vencida").value(false));

        mvc.perform(get("/api/tareas").param("vencidas", "true").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }
}
