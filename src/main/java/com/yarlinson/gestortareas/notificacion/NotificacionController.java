package com.yarlinson.gestortareas.notificacion;

import com.yarlinson.gestortareas.common.PaginaResponse;
import com.yarlinson.gestortareas.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Avisos de tareas por vencer y vencidas del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionController {

    private final NotificacionService servicio;

    public NotificacionController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista las notificaciones propias (más recientes primero); leida=false muestra solo las pendientes")
    public PaginaResponse<NotificacionResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) Boolean leida,
            @ParameterObject
            @PageableDefault(size = 20, sort = "creadaEn", direction = Sort.Direction.DESC) Pageable paginacion) {
        return PaginaResponse.desde(servicio.listar(usuario.getId(), leida, paginacion));
    }

    @PatchMapping("/{id}/leida")
    @Operation(summary = "Marca una notificación como leída")
    public NotificacionResponse marcarLeida(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return servicio.marcarLeida(usuario.getId(), id);
    }
}
