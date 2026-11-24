package com.yarlinson.gestortareas.tarea;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TareaRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String titulo,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        LocalDate fechaLimite) {
}
