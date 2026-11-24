package com.yarlinson.gestortareas.admin;

public record EstadisticasResponse(
        long totalUsuarios,
        long totalTareas,
        long tareasCompletadas,
        long tareasPendientes,
        long tareasVencidas) {
}
