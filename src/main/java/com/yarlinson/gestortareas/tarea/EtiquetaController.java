package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/etiquetas")
@Tag(name = "Etiquetas", description = "Etiquetas del usuario autenticado; se crean al asignarlas a una tarea")
@SecurityRequirement(name = "bearerAuth")
public class EtiquetaController {

    private final EtiquetaService servicio;

    public EtiquetaController(EtiquetaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista las etiquetas propias con el número de tareas que las usan")
    public List<EtiquetaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servicio.listar(usuario.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una etiqueta y la quita de las tareas que la tenían")
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        servicio.eliminar(usuario.getId(), id);
    }
}
