package com.yarlinson.gestortareas.tarea;

import java.time.Instant;
import java.time.LocalDate;

public record TareaResponse(
        Long id,
        String titulo,
        String descripcion,
        boolean completada,
        LocalDate fechaLimite,
        Instant creadaEn,
        Instant actualizadaEn) {

    public static TareaResponse desde(Tarea tarea) {
        return new TareaResponse(
                tarea.getId(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.isCompletada(),
                tarea.getFechaLimite(),
                tarea.getCreadaEn(),
                tarea.getActualizadaEn());
    }
}
