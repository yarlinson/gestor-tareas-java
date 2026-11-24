package com.yarlinson.gestortareas.tarea;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una tarea al crearla o reemplazarla. Si no se indica la prioridad se usa
 * {@code MEDIA}; las etiquetas se crean para el usuario si todavía no existen.
 */
public record TareaRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        LocalDate fechaLimite,

        Prioridad prioridad,

        @Size(max = 10, message = "Una tarea admite como máximo 10 etiquetas")
        List<@NotBlank(message = "Las etiquetas no pueden estar vacías")
             @Size(max = 30, message = "Cada etiqueta admite como máximo 30 caracteres") String> etiquetas) {
}
