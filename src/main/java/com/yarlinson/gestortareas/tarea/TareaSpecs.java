package com.yarlinson.gestortareas.tarea;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros dinámicos para la búsqueda de tareas. Los métodos que reciben un criterio
 * opcional devuelven {@code null} cuando no hay criterio, y {@link Specification#allOf}
 * ignora los valores nulos.
 */
public final class TareaSpecs {

    private static final char ESCAPE = '\\';

    private TareaSpecs() {
    }

    public static Specification<Tarea> deUsuario(Long usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<Tarea> conEstado(Boolean completada) {
        if (completada == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("completada"), completada);
    }

    public static Specification<Tarea> contieneTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String patron = "%" + escaparComodines(texto.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("titulo")), patron, ESCAPE),
                cb.like(cb.lower(root.<String>get("descripcion")), patron, ESCAPE));
    }

    /** Evita que el usuario use '%' o '_' como comodines del LIKE. */
    private static String escaparComodines(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
