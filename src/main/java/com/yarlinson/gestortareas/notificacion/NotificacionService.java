package com.yarlinson.gestortareas.notificacion;

import com.yarlinson.gestortareas.common.RecursoNoEncontradoException;
import com.yarlinson.gestortareas.tarea.FechaLimiteCambiada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    private final NotificacionRepository notificaciones;

    public NotificacionService(NotificacionRepository notificaciones) {
        this.notificaciones = notificaciones;
    }

    @Transactional(readOnly = true)
    public Page<NotificacionResponse> listar(Long usuarioId, Boolean leida, Pageable paginacion) {
        Page<Notificacion> pagina = (leida == null)
                ? notificaciones.findByUsuarioId(usuarioId, paginacion)
                : notificaciones.findByUsuarioIdAndLeida(usuarioId, leida, paginacion);
        return pagina.map(NotificacionResponse::desde);
    }

    @Transactional
    public NotificacionResponse marcarLeida(Long usuarioId, Long id) {
        Notificacion notificacion = notificaciones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación " + id + " no encontrada"));
        notificacion.marcarLeida();
        return NotificacionResponse.desde(notificacion);
    }

    /**
     * Si cambia la fecha límite, los avisos anteriores ya no valen: se borran para que la tarea
     * pueda volver a avisar según su nueva fecha. Se ejecuta en la transacción de quien publica.
     */
    @EventListener
    public void alCambiarLaFechaLimite(FechaLimiteCambiada evento) {
        notificaciones.eliminarDeLaTarea(evento.tareaId());
    }
}
