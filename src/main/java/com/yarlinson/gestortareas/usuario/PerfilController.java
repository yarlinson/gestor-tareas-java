package com.yarlinson.gestortareas.usuario;

import com.yarlinson.gestortareas.common.RecursoNoEncontradoException;
import com.yarlinson.gestortareas.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Perfil", description = "Datos del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {

    private final UsuarioRepository usuarios;

    public PerfilController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    @Operation(summary = "Devuelve el usuario autenticado y su rol (útil para que un cliente sepa qué mostrar)")
    public PerfilResponse perfil(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return usuarios.findById(principal.getId())
                .map(PerfilResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
