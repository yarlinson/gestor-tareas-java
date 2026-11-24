package com.yarlinson.gestortareas.admin;

import com.yarlinson.gestortareas.usuario.Rol;

import java.time.Instant;

/** Usuario tal como lo ve un administrador (nunca incluye la clave). */
public record UsuarioAdminResponse(
        Long id,
        String nombreUsuario,
        String email,
        Rol rol,
        Instant creadoEn,
        Long totalTareas) {
}
