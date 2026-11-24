package com.yarlinson.gestortareas.tarea;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "El campo 'completada' es obligatorio")
        Boolean completada) {
}
