package com.yarlinson.gestortareas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelojConfig {

    /**
     * Reloj de la aplicación. La zona horaria decide qué día es "hoy" al calcular tareas vencidas
     * y recordatorios (el servidor suele estar en UTC, que no coincide con el día local).
     */
    @Bean
    Clock reloj(@Value("${app.timezone}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
