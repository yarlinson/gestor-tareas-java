package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.common.PaginaResponse;
import com.yarlinson.gestortareas.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/tareas")
@Tag(name = "Tareas", description = "CRUD de tareas del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class TareaController {

    private final TareaService servicio;

    public TareaController(TareaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista las tareas propias con filtros (estado, texto, prioridad, etiqueta, vencidas), orden y paginación")
    public PaginaResponse<TareaResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) Boolean completada,
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) String etiqueta,
            @RequestParam(required = false) Boolean vencidas,
            @ParameterObject
            @PageableDefault(size = 10, sort = "creadaEn", direction = Sort.Direction.DESC) Pageable paginacion) {
        FiltroTareas filtro = new FiltroTareas(completada, texto, prioridad, etiqueta, vencidas);
        return PaginaResponse.desde(servicio.listar(usuario.getId(), filtro, paginacion));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una tarea propia por su identificador")
    public TareaResponse obtener(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return servicio.obtener(usuario.getId(), id);
    }

    @PostMapping
    @Operation(summary = "Crea una tarea")
    public ResponseEntity<TareaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody TareaRequest solicitud) {
        TareaResponse creada = servicio.crear(usuario.getId(), solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplaza título, descripción y fecha límite de una tarea")
    public TareaResponse actualizar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody TareaRequest solicitud) {
        return servicio.actualizar(usuario.getId(), id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Marca una tarea como completada o pendiente")
    public TareaResponse cambiarEstado(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable Long id,
            @Valid @RequestBody EstadoRequest solicitud) {
        return servicio.cambiarEstado(usuario.getId(), id, solicitud.completada());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una tarea")
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        servicio.eliminar(usuario.getId(), id);
    }
}
