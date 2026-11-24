package com.yarlinson.gestortareas.tarea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TareaResponse(
        Long id,
        String titulo,
        String descripcion,
        boolean completada,
        LocalDate fechaLimite,
        Prioridad prioridad,
        List<String> etiquetas,
        boolean vencida,
        Instant creadaEn,
        Instant actualizadaEn) {

    /** @param hoy fecha de referencia para saber si la tarea está vencida */
    public static TareaResponse desde(Tarea tarea, LocalDate hoy) {
        List<String> etiquetas = tarea.getEtiquetas().stream()
                .map(Etiqueta::getNombre)
                .sorted()
                .toList();
        boolean vencida = !tarea.isCompletada()
                && tarea.getFechaLimite() != null
                && tarea.getFechaLimite().isBefore(hoy);

        return new TareaResponse(
                tarea.getId(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.isCompletada(),
                tarea.getFechaLimite(),
                tarea.getPrioridad(),
                etiquetas,
                vencida,
                tarea.getCreadaEn(),
                tarea.getActualizadaEn());
    }
}
