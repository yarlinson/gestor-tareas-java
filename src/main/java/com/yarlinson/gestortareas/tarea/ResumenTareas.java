package com.yarlinson.gestortareas.tarea;

/**
 * Totales de las tareas del usuario, pensados para un panel de resumen.
 *
 * @param total             todas las tareas
 * @param pendientes        las que aún no están completadas
 * @param hechas            las completadas
 * @param vencidas          pendientes con la fecha límite anterior a hoy
 * @param paraHoy           pendientes cuya fecha límite es hoy
 * @param proximosSieteDias pendientes cuya fecha límite está entre mañana y dentro de siete días
 * @param sinPlazo          pendientes sin fecha límite
 * @param pendientesAlta    pendientes de prioridad alta
 * @param pendientesMedia   pendientes de prioridad media
 * @param pendientesBaja    pendientes de prioridad baja
 */
public record ResumenTareas(
        long total,
        long pendientes,
        long hechas,
        long vencidas,
        long paraHoy,
        long proximosSieteDias,
        long sinPlazo,
        long pendientesAlta,
        long pendientesMedia,
        long pendientesBaja) {
}
