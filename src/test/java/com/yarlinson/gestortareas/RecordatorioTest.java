package com.yarlinson.gestortareas;

import com.fasterxml.jackson.databind.JsonNode;
import com.yarlinson.gestortareas.notificacion.RecordatorioService;
import com.yarlinson.gestortareas.notificacion.ResultadoRecordatorios;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa una fecha fija ("hoy" = 2030-01-10) para no depender del día en que se ejecuten las pruebas. */
class RecordatorioTest extends IntegracionBase {

    private static final String AUTH = "Authorization";
    private static final LocalDate HOY = LocalDate.of(2030, 1, 10);

    @Autowired
    private RecordatorioService recordatorios;

    private long tarea(String token, String titulo, String fechaLimite) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("titulo", titulo);
        cuerpo.put("fechaLimite", fechaLimite);
        String respuesta = mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("id").asLong();
    }

    private JsonNode notificaciones(String token, String consulta) throws Exception {
        String respuesta = mvc.perform(get("/api/notificaciones" + consulta).header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    private static List<String> mensajes(JsonNode pagina) {
        List<String> mensajes = new ArrayList<>();
        pagina.get("contenido").forEach(n -> mensajes.add(n.get("mensaje").asText()));
        return mensajes;
    }

    @Test
    @DisplayName("Avisa de las tareas que vencen hoy o mañana y de las vencidas; ignora el resto")
    void generaLosAvisosCorrectos() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        tarea(token, "Entrega mañana", "2030-01-11");
        tarea(token, "Entrega hoy", "2030-01-10");
        tarea(token, "Lejana", "2030-01-12");
        tarea(token, "Atrasada", "2030-01-09");
        long hecha = tarea(token, "Hecha", "2030-01-09");
        mvc.perform(patch("/api/tareas/{id}/estado", hecha)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completada\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sin fecha\"}"))
                .andExpect(status().isCreated());

        recordatorios.generar(HOY);

        JsonNode pagina = notificaciones(token, "");
        assertThat(pagina.get("totalElementos").asInt()).isEqualTo(3);
        assertThat(mensajes(pagina)).containsExactlyInAnyOrder(
                "La tarea 'Entrega mañana' vence mañana.",
                "La tarea 'Entrega hoy' vence hoy.",
                "La tarea 'Atrasada' venció el 2030-01-09.");
        assertThat(pagina.get("contenido").findValuesAsText("tipo"))
                .containsExactlyInAnyOrder("POR_VENCER", "POR_VENCER", "VENCIDA");
    }

    @Test
    @DisplayName("Es idempotente: ejecutarlo de nuevo no repite avisos")
    void noRepiteAvisos() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        tarea(token, "Atrasada", "2030-01-09");
        tarea(token, "Entrega hoy", "2030-01-10");

        ResultadoRecordatorios primera = recordatorios.generar(HOY);
        ResultadoRecordatorios segunda = recordatorios.generar(HOY);

        assertThat(primera.porVencer() + primera.vencidas()).isGreaterThanOrEqualTo(2);
        assertThat(segunda).isEqualTo(new ResultadoRecordatorios(0, 0));
        assertThat(notificaciones(token, "").get("totalElementos").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cada usuario ve solo sus avisos y puede marcarlos como leídos")
    void avisosPorUsuarioYLectura() throws Exception {
        String tokenA = registrarYObtenerToken(nombreUnico());
        String tokenB = registrarYObtenerToken(nombreUnico());
        tarea(tokenA, "Atrasada A", "2030-01-09");
        tarea(tokenA, "Entrega hoy A", "2030-01-10");
        recordatorios.generar(HOY);

        assertThat(notificaciones(tokenB, "").get("totalElementos").asInt()).isZero();

        JsonNode deA = notificaciones(tokenA, "?leida=false");
        assertThat(deA.get("totalElementos").asInt()).isEqualTo(2);
        long id = deA.get("contenido").get(0).get("id").asLong();

        mvc.perform(patch("/api/notificaciones/{id}/leida", id).header(AUTH, bearer(tokenB)))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/notificaciones/{id}/leida", id).header(AUTH, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));

        assertThat(notificaciones(tokenA, "?leida=false").get("totalElementos").asInt()).isEqualTo(1);
        assertThat(notificaciones(tokenA, "?leida=true").get("totalElementos").asInt()).isEqualTo(1);
        assertThat(notificaciones(tokenA, "").get("totalElementos").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Si cambia la fecha límite, la tarea vuelve a avisar según la fecha nueva")
    void cambiarLaFechaReactivaElAviso() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        long id = tarea(token, "Se reprograma", "2030-01-09");
        recordatorios.generar(HOY);
        assertThat(mensajes(notificaciones(token, ""))).containsExactly("La tarea 'Se reprograma' venció el 2030-01-09.");

        mvc.perform(put("/api/tareas/{id}", id)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Se reprograma\",\"fechaLimite\":\"2030-01-05\"}"))
                .andExpect(status().isOk());
        assertThat(notificaciones(token, "").get("totalElementos").asInt()).isZero();

        recordatorios.generar(HOY);
        assertThat(mensajes(notificaciones(token, ""))).containsExactly("La tarea 'Se reprograma' venció el 2030-01-05.");
    }

    @Test
    @DisplayName("Editar una tarea sin cambiar su fecha no borra sus avisos")
    void editarSinCambiarLaFechaConservaLosAvisos() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        long id = tarea(token, "Igual", "2030-01-09");
        recordatorios.generar(HOY);

        mvc.perform(put("/api/tareas/{id}", id)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Igual pero con otro título\",\"fechaLimite\":\"2030-01-09\"}"))
                .andExpect(status().isOk());

        assertThat(notificaciones(token, "").get("totalElementos").asInt()).isEqualTo(1);
    }
}
