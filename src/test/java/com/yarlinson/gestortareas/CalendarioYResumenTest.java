package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarioYResumenTest extends IntegracionBase {

    private static final String AUTH = "Authorization";
    /** La misma zona que usa la aplicación por defecto (app.timezone) para decidir qué día es "hoy". */
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private long crear(String token, String titulo, String prioridad, LocalDate fecha) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("titulo", titulo);
        cuerpo.put("prioridad", prioridad);
        if (fecha != null) {
            cuerpo.put("fechaLimite", fecha.toString());
        }
        String respuesta = mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("id").asLong();
    }

    @Test
    @DisplayName("Filtra por rango de fechas límite con los dos extremos incluidos")
    void filtraPorRangoDeFechas() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        crear(token, "Fin de febrero", "MEDIA", LocalDate.of(2031, 2, 28));
        crear(token, "Inicio de marzo", "MEDIA", LocalDate.of(2031, 3, 1));
        crear(token, "Mitad de marzo", "MEDIA", LocalDate.of(2031, 3, 15));
        crear(token, "Fin de marzo", "MEDIA", LocalDate.of(2031, 3, 31));
        crear(token, "Inicio de abril", "MEDIA", LocalDate.of(2031, 4, 1));
        crear(token, "Sin fecha", "MEDIA", null);

        mvc.perform(get("/api/tareas")
                        .param("desde", "2031-03-01").param("hasta", "2031-03-31")
                        .param("sort", "fechaLimite,asc")
                        .header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Inicio de marzo"))
                .andExpect(jsonPath("$.contenido[1].titulo").value("Mitad de marzo"))
                .andExpect(jsonPath("$.contenido[2].titulo").value("Fin de marzo"));

        // Con un solo extremo: las tareas sin fecha nunca entran en un rango.
        mvc.perform(get("/api/tareas").param("desde", "2031-03-31").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/tareas").param("hasta", "2031-03-01").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(2));

        mvc.perform(get("/api/tareas").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    @DisplayName("Lista solo las tareas sin fecha límite, o solo las que la tienen")
    void filtraTareasSinFecha() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        crear(token, "Con fecha", "MEDIA", LocalDate.of(2031, 5, 5));
        crear(token, "Sin fecha uno", "MEDIA", null);
        crear(token, "Sin fecha dos", "MEDIA", null);

        mvc.perform(get("/api/tareas").param("sinFecha", "true").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/tareas").param("sinFecha", "false").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Con fecha"));
    }

    @Test
    @DisplayName("Una fecha mal escrita devuelve 400")
    void rechazaFechasMalFormadas() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        mvc.perform(get("/api/tareas").param("desde", "ayer").header(AUTH, bearer(token)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tareas").param("hasta", "31/03/2031").header(AUTH, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El resumen cuenta cada grupo de tareas propias")
    void resumenCuentaCadaGrupo() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        LocalDate hoy = LocalDate.now(ZONA);

        crear(token, "Atrasada", "ALTA", hoy.minusDays(3));
        crear(token, "De hoy", "MEDIA", hoy);
        crear(token, "Mañana", "ALTA", hoy.plusDays(1));
        crear(token, "En cinco días", "BAJA", hoy.plusDays(5));
        crear(token, "En un mes", "MEDIA", hoy.plusDays(30));
        crear(token, "Sin fecha", "BAJA", null);
        long hecha = crear(token, "Hecha", "ALTA", hoy.minusDays(10));
        mvc.perform(patch("/api/tareas/{id}/estado", hecha)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completada\":true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/tareas/resumen").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.pendientes").value(6))
                .andExpect(jsonPath("$.hechas").value(1))
                // La tarea hecha con fecha pasada no cuenta como vencida.
                .andExpect(jsonPath("$.vencidas").value(1))
                .andExpect(jsonPath("$.paraHoy").value(1))
                .andExpect(jsonPath("$.proximosSieteDias").value(2))
                .andExpect(jsonPath("$.sinPlazo").value(1))
                .andExpect(jsonPath("$.pendientesAlta").value(2))
                .andExpect(jsonPath("$.pendientesMedia").value(2))
                .andExpect(jsonPath("$.pendientesBaja").value(2));
    }

    @Test
    @DisplayName("Cada usuario ve solo su propio resumen, y sin token responde 401")
    void resumenEsDeCadaUsuario() throws Exception {
        String tokenA = registrarYObtenerToken(nombreUnico());
        String tokenB = registrarYObtenerToken(nombreUnico());
        crear(tokenA, "De A", "ALTA", null);

        mvc.perform(get("/api/tareas/resumen").header(AUTH, bearer(tokenA)))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/tareas/resumen").header(AUTH, bearer(tokenB)))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.pendientes").value(0));
        mvc.perform(get("/api/tareas/resumen")).andExpect(status().isUnauthorized());
    }
}
