package com.yarlinson.gestortareas.notificacion;

import java.time.Instant;

public record NotificacionResponse(
        Long id,
        Long tareaId,
        TipoNotificacion tipo,
        String mensaje,
        boolean leida,
        Instant creadaEn) {

    public static NotificacionResponse desde(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTarea().getId(),
                notificacion.getTipo(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getCreadaEn());
    }
}
