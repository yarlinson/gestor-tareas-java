package com.yarlinson.gestortareas;

import com.yarlinson.gestortareas.notificacion.RecordatorioProgramador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * En el resto de pruebas el proceso programado está desactivado. Aquí se activa para comprobar
 * que la aplicación arranca con su configuración real (resolución de {@code ${app.recordatorios.*}}
 * y de la zona horaria en {@code @Scheduled}) y que la ejecución al arrancar no falla.
 * El cron se desactiva ("-") para que no se dispare en mitad de otras pruebas.
 */
@TestPropertySource(properties = {
        "app.recordatorios.enabled=true",
        "app.recordatorios.cron=-"
})
class RecordatorioProgramadorTest extends IntegracionBase {

    @Autowired(required = false)
    private RecordatorioProgramador programador;

    @Test
    @DisplayName("Con los recordatorios activados, el programador se registra y se puede ejecutar")
    void elProgramadorArrancaYSePuedeEjecutar() {
        assertThat(programador).isNotNull();
        assertThatCode(programador::generarPeriodicamente).doesNotThrowAnyException();
    }
}
