package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TareaControllerTest extends IntegracionBase {

    private static final String AUTH = "Authorization";

    private String cuerpoEstado(boolean completada) throws Exception {
        return json.writeValueAsString(Map.of("completada", completada));
    }

    @Test
    @DisplayName("Sin token las rutas de tareas devuelven 401")
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/tareas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTarea("Algo", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Crear una tarea devuelve 201, Location y la tarea pendiente")
    void creaTarea() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTarea("  Estudiar Spring Security  ", "Capitulo de JWT")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Estudiar Spring Security"))
                .andExpect(jsonPath("$.descripcion").value("Capitulo de JWT"))
                .andExpect(jsonPath("$.completada").value(false))
                .andExpect(jsonPath("$.creadaEn").isNotEmpty());
    }

    @Test
    @DisplayName("Un título vacío devuelve 400 con el detalle del campo")
    void tituloObligatorio() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        mvc.perform(post("/api/tareas")
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTarea("   ", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").value("El título es obligatorio"));
    }

    @Test
    @DisplayName("Cada usuario solo ve sus propias tareas")
    void listaSoloTareasPropias() throws Exception {
        String tokenA = registrarYObtenerToken(nombreUnico());
        String tokenB = registrarYObtenerToken(nombreUnico());
        crearTarea(tokenA, "Tarea A1", null);
        crearTarea(tokenA, "Tarea A2", null);
        crearTarea(tokenB, "Tarea B1", null);

        mvc.perform(get("/api/tareas").header(AUTH, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(2));

        mvc.perform(get("/api/tareas").header(AUTH, bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Tarea B1"));
    }

    @Test
    @DisplayName("Una tarea ajena responde 404 en lectura, edición, cambio de estado y borrado")
    void noSePuedeAccederATareasAjenas() throws Exception {
        String tokenA = registrarYObtenerToken(nombreUnico());
        String tokenB = registrarYObtenerToken(nombreUnico());
        long id = crearTarea(tokenA, "Tarea privada", null);

        mvc.perform(get("/api/tareas/{id}", id).header(AUTH, bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/tareas/{id}", id)
                        .header(AUTH, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTarea("Robada", null)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/tareas/{id}/estado", id)
                        .header(AUTH, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado(true)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tareas/{id}", id).header(AUTH, bearer(tokenB)))
                .andExpect(status().isNotFound());

        // La tarea sigue intacta para su propietario.
        mvc.perform(get("/api/tareas/{id}", id).header(AUTH, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Tarea privada"))
                .andExpect(jsonPath("$.completada").value(false));
    }

    @Test
    @DisplayName("Marcar como completada y filtrar por estado")
    void filtraPorEstado() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        long pendiente = crearTarea(token, "Pendiente", null);
        long hecha = crearTarea(token, "Hecha", null);

        mvc.perform(patch("/api/tareas/{id}/estado", hecha)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completada").value(true));

        mvc.perform(get("/api/tareas").param("completada", "true").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(hecha));

        mvc.perform(get("/api/tareas").param("completada", "false").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(pendiente));
    }

    @Test
    @DisplayName("Busca por texto en título y descripción, sin distinguir mayúsculas")
    void buscaPorTexto() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        crearTarea(token, "Comprar leche", "Ir al supermercado");
        crearTarea(token, "Pagar recibos", null);

        mvc.perform(get("/api/tareas").param("q", "SUPERMERCADO").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Comprar leche"));

        mvc.perform(get("/api/tareas").param("q", "recibos").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Pagar recibos"));

        // '%' se trata como texto literal, no como comodín que coincida con todo.
        mvc.perform(get("/api/tareas").param("q", "%").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Pagina y ordena los resultados")
    void paginaYOrdena() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        crearTarea(token, "Bravo", null);
        crearTarea(token, "Alfa", null);
        crearTarea(token, "Charlie", null);

        mvc.perform(get("/api/tareas")
                        .param("size", "2").param("page", "0").param("sort", "titulo,asc")
                        .header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Alfa"))
                .andExpect(jsonPath("$.contenido[1].titulo").value("Bravo"));

        mvc.perform(get("/api/tareas")
                        .param("size", "2").param("page", "1").param("sort", "titulo,asc")
                        .header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Charlie"));
    }

    @Test
    @DisplayName("Un campo de ordenamiento inexistente devuelve 400")
    void ordenamientoInvalido() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        mvc.perform(get("/api/tareas").param("sort", "noExiste,asc").header(AUTH, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Actualizar reemplaza título, descripción y fecha límite")
    void actualizaTarea() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        long id = crearTarea(token, "Original", "Texto original");

        String cuerpo = json.writeValueAsString(Map.of(
                "titulo", "Modificada",
                "descripcion", "Texto nuevo",
                "fechaLimite", "2030-01-31"));

        mvc.perform(put("/api/tareas/{id}", id)
                        .header(AUTH, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Modificada"))
                .andExpect(jsonPath("$.descripcion").value("Texto nuevo"))
                .andExpect(jsonPath("$.fechaLimite").value("2030-01-31"));

        mvc.perform(get("/api/tareas/{id}", id).header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.titulo").value("Modificada"));
    }

    @Test
    @DisplayName("Eliminar devuelve 204 y la tarea deja de existir")
    void eliminaTarea() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());
        long id = crearTarea(token, "Para borrar", null);

        mvc.perform(delete("/api/tareas/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/tareas/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isNotFound());
    }
}
