package com.yarlinson.gestortareas.tarea;

import java.time.LocalDate;

/**
 * Criterios opcionales para listar tareas; un campo nulo significa "no filtrar por esto".
 *
 * @param completada estado de la tarea
 * @param texto      texto a buscar en título y descripción
 * @param prioridad  prioridad exacta
 * @param etiqueta   nombre de una etiqueta que la tarea debe tener
 * @param vencidas   {@code true} solo vencidas (con fecha límite pasada y sin completar), {@code false} las demás
 * @param desde      fecha límite mínima (inclusive); excluye las tareas sin fecha
 * @param hasta      fecha límite máxima (inclusive); excluye las tareas sin fecha
 * @param sinFecha   {@code true} solo las tareas sin fecha límite, {@code false} solo las que la tienen
 */
public record FiltroTareas(
        Boolean completada,
        String texto,
        Prioridad prioridad,
        String etiqueta,
        Boolean vencidas,
        LocalDate desde,
        LocalDate hasta,
        Boolean sinFecha) {
}
