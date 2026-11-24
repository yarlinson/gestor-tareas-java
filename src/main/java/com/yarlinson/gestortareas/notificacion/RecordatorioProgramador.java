package com.yarlinson.gestortareas.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta el proceso de recordatorios de forma periódica (por defecto cada hora en punto) y una
 * vez al arrancar. La ejecución al arrancar es importante en planes gratuitos donde el servicio
 * se duerme por inactividad: al despertar se ponen al día los avisos que se hubieran perdido.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.recordatorios", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecordatorioProgramador {

    private static final Logger LOG = LoggerFactory.getLogger(RecordatorioProgramador.class);

    private final RecordatorioService servicio;

    public RecordatorioProgramador(RecordatorioService servicio) {
        this.servicio = servicio;
    }

    @Scheduled(cron = "${app.recordatorios.cron}", zone = "${app.timezone}")
    public void generarPeriodicamente() {
        ejecutar("programada");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generarAlArrancar() {
        ejecutar("al arrancar");
    }

    private void ejecutar(String origen) {
        try {
            ResultadoRecordatorios resultado = servicio.generar();
            LOG.info("Recordatorios ({}): {} avisos por vencer y {} de tareas vencidas",
                    origen, resultado.porVencer(), resultado.vencidas());
        } catch (RuntimeException ex) {
            // Un fallo (por ejemplo, la base de datos aún despertando) no debe tumbar la aplicación.
            LOG.error("No se pudieron generar los recordatorios ({})", origen, ex);
        }
    }
}
