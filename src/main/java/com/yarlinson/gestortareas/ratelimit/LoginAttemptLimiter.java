package com.yarlinson.gestortareas.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yarlinson.gestortareas.common.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

/**
 * Limita los intentos de inicio de sesión <b>por nombre de usuario</b>, sin importar desde qué
 * IP lleguen. Es la defensa principal contra la fuerza bruta sobre una cuenta y no depende de
 * poder identificar al cliente detrás de un proxy; el límite por IP ({@link RateLimitFilter})
 * la complementa frenando el abuso masivo.
 * <p>
 * Contrapartida: quien insista con el nombre de otra persona puede retrasarle el acceso, pero
 * solo hasta que la cubeta se recupere (unos segundos), no la bloquea de forma permanente.
 */
@Component
public class LoginAttemptLimiter {

    private static final int MAXIMO_DE_USUARIOS_EN_MEMORIA = 10_000;

    private final RateLimitProperties propiedades;
    private final Cache<String, Bucket> cubetas;

    public LoginAttemptLimiter(RateLimitProperties propiedades) {
        this.propiedades = propiedades;
        this.cubetas = Caffeine.newBuilder()
                .expireAfterAccess(propiedades.window())
                .maximumSize(MAXIMO_DE_USUARIOS_EN_MEMORIA)
                .build();
    }

    /**
     * Cuenta un intento para el usuario indicado.
     *
     * @throws TooManyRequestsException si el usuario ya agotó su cupo en la ventana actual
     */
    public void registrarIntento(String nombreUsuario) {
        if (!propiedades.enabled()) {
            return;
        }
        Bucket cubeta = cubetas.get(nombreUsuario, clave -> nuevaCubeta());
        ConsumptionProbe resultado = cubeta.tryConsumeAndReturnRemaining(1);
        if (!resultado.isConsumed()) {
            long segundos = TiempoDeEspera.enSegundos(resultado.getNanosToWaitForRefill());
            throw new TooManyRequestsException(
                    "Demasiados intentos para este usuario. Vuelva a intentarlo en " + segundos + " segundos.",
                    segundos);
        }
    }

    private Bucket nuevaCubeta() {
        return Bucket.builder()
                .addLimit(limite -> limite
                        .capacity(propiedades.loginCapacityPerUser())
                        .refillGreedy(propiedades.loginCapacityPerUser(), propiedades.window()))
                .build();
    }
}
