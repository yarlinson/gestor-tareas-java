package com.yarlinson.gestortareas.usuario;

/** Datos del usuario autenticado. Nunca incluye la clave. */
public record PerfilResponse(Long id, String nombreUsuario, String email, Rol rol) {

    public static PerfilResponse desde(Usuario usuario) {
        return new PerfilResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getEmail(), usuario.getRol());
    }
}
