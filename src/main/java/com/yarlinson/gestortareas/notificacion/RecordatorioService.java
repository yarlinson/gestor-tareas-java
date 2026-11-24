package com.yarlinson.gestortareas.notificacion;

import com.yarlinson.gestortareas.tarea.Tarea;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

/**
 * Crea avisos para las tareas sin completar que están por vencer o ya vencieron. Es idempotente:
 * una tarea recibe como máximo un aviso de cada tipo (lo garantiza una restricción única en la
 * base de datos), así que se puede ejecutar tantas veces como se quiera.
 * <p>
 * Está pensado para una sola instancia. Con varias ejecutándose a la vez, la restricción única
 * evita avisos duplicados, pero una de las ejecuciones fallaría y repetiría en la siguiente pasada.
 */
@Service
public class RecordatorioService {

    private static final int TAMANO_DEL_LOTE = 500;

    private final RecordatorioRepository tareasPendientes;
    private final NotificacionRepository notificaciones;
    private final Clock reloj;
    private final int diasDeAnticipacion;

    public RecordatorioService(
            RecordatorioRepository tareasPendientes,
            NotificacionRepository notificaciones,
            Clock reloj,
            @Value("${app.recordatorios.days-ahead}") int diasDeAnticipacion) {
        this.tareasPendientes = tareasPendientes;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
        this.diasDeAnticipacion = diasDeAnticipacion;
    }

    /** Genera los avisos tomando como "hoy" la fecha actual en la zona horaria configurada. */
    @Transactional
    public ResultadoRecordatorios generar() {
        return generar(LocalDate.now(reloj));
    }

    @Transactional
    public ResultadoRecordatorios generar(LocalDate hoy) {
        LocalDate limite = hoy.plusDays(diasDeAnticipacion);

        int porVencer = crearAvisos(
                TipoNotificacion.POR_VENCER,
                lote -> tareasPendientes.sinAvisoDeVencimientoProximo(hoy, limite, TipoNotificacion.POR_VENCER, lote),
                tarea -> mensajeDeVencimientoProximo(tarea, hoy));

        int vencidas = crearAvisos(
                TipoNotificacion.VENCIDA,
                lote -> tareasPendientes.sinAvisoDeVencida(hoy, TipoNotificacion.VENCIDA, lote),
                tarea -> "La tarea '" + tarea.getTitulo() + "' venció el " + tarea.getFechaLimite() + ".");

        return new ResultadoRecordatorios(porVencer, vencidas);
    }

    /** Procesa por lotes; al guardar los avisos las tareas dejan de aparecer en la consulta. */
    private int crearAvisos(
            TipoNotificacion tipo,
            Function<Pageable, List<Tarea>> consulta,
            Function<Tarea, String> mensaje) {
        int creados = 0;
        List<Tarea> lote;
        do {
            lote = consulta.apply(PageRequest.of(0, TAMANO_DEL_LOTE));
            for (Tarea tarea : lote) {
                notificaciones.save(new Notificacion(tarea.getUsuario(), tarea, tipo, mensaje.apply(tarea)));
            }
            notificaciones.flush();
            creados += lote.size();
        } while (lote.size() == TAMANO_DEL_LOTE);
        return creados;
    }

    private static String mensajeDeVencimientoProximo(Tarea tarea, LocalDate hoy) {
        long dias = ChronoUnit.DAYS.between(hoy, tarea.getFechaLimite());
        String cuando = dias == 0 ? "vence hoy"
                : dias == 1 ? "vence mañana"
                : "vence el " + tarea.getFechaLimite();
        return "La tarea '" + tarea.getTitulo() + "' " + cuando + ".";
    }
}
