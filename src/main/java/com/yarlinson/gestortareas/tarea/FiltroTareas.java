package com.yarlinson.gestortareas.tarea;

/**
 * Criterios opcionales para listar tareas; un campo nulo significa "no filtrar por esto".
 *
 * @param completada estado de la tarea
 * @param texto      texto a buscar en título y descripción
 * @param prioridad  prioridad exacta
 * @param etiqueta   nombre de una etiqueta que la tarea debe tener
 * @param vencidas   {@code true} solo vencidas (con fecha límite pasada y sin completar), {@code false} las demás
 */
public record FiltroTareas(
        Boolean completada,
        String texto,
        Prioridad prioridad,
        String etiqueta,
        Boolean vencidas) {
}
