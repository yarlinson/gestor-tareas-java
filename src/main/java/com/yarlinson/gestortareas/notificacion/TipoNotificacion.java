package com.yarlinson.gestortareas.notificacion;

public enum TipoNotificacion {
    /** La fecha límite es hoy o está dentro de los días de anticipación configurados. */
    POR_VENCER,
    /** La fecha límite ya pasó y la tarea sigue sin completarse. */
    VENCIDA
}
