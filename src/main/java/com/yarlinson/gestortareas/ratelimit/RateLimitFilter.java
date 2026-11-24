package com.yarlinson.gestortareas.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Limita los intentos por IP en {@code /api/auth/**} (registro e inicio de sesión) para
 * frenar la fuerza bruta y el registro masivo. Usa el algoritmo "token bucket" de Bucket4j:
 * cada IP tiene una cubeta con {@code capacity} cupos que se recupera de forma gradual
 * durante {@code window}.
 * <p>
 * Las cubetas viven en memoria (Caffeine), con un máximo de IPs y expiración por
 * inactividad. Con varias instancias de la aplicación habría que compartir el estado
 * (por ejemplo en Redis); para una sola instancia es suficiente.
 * <p>
 * No se anota como {@code @Component} para que Spring Boot no lo registre dos veces: se
 * añade solo a la cadena de seguridad.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PREFIJO_PROTEGIDO = "/api/auth/";
    private static final int MAXIMO_DE_IPS_EN_MEMORIA = 10_000;
    private static final long NANOS_POR_SEGUNDO = 1_000_000_000L;

    private final RateLimitProperties propiedades;
    private final Cache<String, Bucket> cubetas;

    public RateLimitFilter(RateLimitProperties propiedades) {
        this.propiedades = propiedades;
        // Pasada una ventana sin actividad la cubeta ya estaría llena, así que se puede descartar.
        this.cubetas = Caffeine.newBuilder()
                .expireAfterAccess(propiedades.window())
                .maximumSize(MAXIMO_DE_IPS_EN_MEMORIA)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !propiedades.enabled() || !request.getRequestURI().startsWith(PREFIJO_PROTEGIDO);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain cadena) throws ServletException, IOException {

        Bucket cubeta = cubetas.get(request.getRemoteAddr(), ip -> nuevaCubeta());
        ConsumptionProbe resultado = cubeta.tryConsumeAndReturnRemaining(1);

        if (resultado.isConsumed()) {
            cadena.doFilter(request, response);
            return;
        }

        long segundosDeEspera = Math.max(1,
                (resultado.getNanosToWaitForRefill() + NANOS_POR_SEGUNDO - 1) / NANOS_POR_SEGUNDO);
        responderDemasiadosIntentos(response, segundosDeEspera);
    }

    private Bucket nuevaCubeta() {
        return Bucket.builder()
                .addLimit(limite -> limite
                        .capacity(propiedades.capacity())
                        .refillGreedy(propiedades.capacity(), propiedades.window()))
                .build();
    }

    private void responderDemasiadosIntentos(HttpServletResponse response, long segundosDeEspera)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundosDeEspera));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Demasiados intentos. Vuelva a intentarlo en %d segundos."}"""
                .formatted(segundosDeEspera));
    }
}
