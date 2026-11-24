package com.yarlinson.gestortareas.admin;

import com.yarlinson.gestortareas.common.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Solo para usuarios con rol ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private static final int TAMANO_MAXIMO = 100;

    private final AdminService servicio;

    public AdminController(AdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Lista los usuarios (más recientes primero) con su número de tareas")
    public PaginaResponse<UsuarioAdminResponse> usuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // El orden es fijo: ordenar por el conteo de tareas no es una propiedad de la entidad.
        PageRequest paginacion = PageRequest.of(
                Math.max(0, page),
                Math.min(TAMANO_MAXIMO, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "creadoEn"));
        return PaginaResponse.desde(servicio.listarUsuarios(paginacion));
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Totales de usuarios y tareas de toda la aplicación")
    public EstadisticasResponse estadisticas() {
        return servicio.estadisticas();
    }
}
