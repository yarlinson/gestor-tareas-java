package com.yarlinson.gestortareas.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración del límite de intentos por IP (propiedades {@code app.rate-limit.*}).
 *
 * @param enabled  activa o desactiva el límite
 * @param capacity intentos permitidos por IP dentro de la ventana
 * @param window         ventana en la que se recupera el cupo completo (por ejemplo {@code 1m})
 * @param clientIpHeader cabecera de la que se toma la IP real del cliente cuando hay proxies
 *                       delante (por ejemplo {@code CF-Connecting-IP}). Vacío usa la IP de la
 *                       conexión. Solo debe indicarse una cabecera que fije el proxy y que el
 *                       cliente no pueda falsear; si no, cualquiera evitaría el límite.
 * @param debug          registra en el log la IP de la conexión y las cabeceras de reenvío
 *                       para diagnosticar; no activar de forma permanente
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("10") int capacity,
        @DefaultValue("1m") Duration window,
        @DefaultValue("") String clientIpHeader,
        @DefaultValue("false") boolean debug) {

    public RateLimitProperties {
        if (capacity < 1) {
            throw new IllegalArgumentException("app.rate-limit.capacity debe ser al menos 1");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("app.rate-limit.window debe ser una duración positiva");
        }
    }
}
