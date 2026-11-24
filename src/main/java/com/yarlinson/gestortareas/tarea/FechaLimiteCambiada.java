package com.yarlinson.gestortareas.tarea;

/**
 * Evento que se publica cuando cambia la fecha límite de una tarea. Permite que otros módulos
 * (por ejemplo, las notificaciones) reaccionen sin que el módulo de tareas dependa de ellos.
 */
public record FechaLimiteCambiada(Long tareaId) {
}
