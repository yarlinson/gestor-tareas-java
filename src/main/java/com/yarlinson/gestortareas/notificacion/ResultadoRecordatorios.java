package com.yarlinson.gestortareas.notificacion;

/** Cuántos avisos nuevos creó una ejecución del proceso de recordatorios. */
public record ResultadoRecordatorios(int porVencer, int vencidas) {
}
