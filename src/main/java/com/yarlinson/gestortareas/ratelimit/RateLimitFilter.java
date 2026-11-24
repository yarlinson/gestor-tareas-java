package com.yarlinson.gestortareas.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Limita los intentos por cliente en {@code /api/auth/**} (registro e inicio de sesión) para
 * frenar la fuerza bruta y el registro masivo. Usa el algoritmo "token bucket" de Bucket4j:
 * cada cliente tiene una cubeta con {@code capacity} cupos que se recupera de forma gradual
 * durante {@code window}.
 * <p>
 * <b>Identificación del cliente.</b> Por defecto se usa la IP de la conexión. Detrás de un
 * proxy esa IP es la del proxy (y en plataformas como Render cambia de una petición a otra),
 * así que el límite no sería efectivo: en ese caso se configura {@code client-ip-header} con
 * la cabecera donde el proxy deja la IP real. Esa cabecera <b>debe</b> ser una que el proxy
 * sobrescriba; si el cliente pudiera elegir su valor, evitaría el límite cambiándola.
 * <p>
 * Las cubetas viven en memoria (Caffeine), con un máximo de clientes y expiración por
 * inactividad. Con varias instancias de la aplicación habría que compartir el estado
 * (por ejemplo en Redis); para una sola instancia es suficiente.
 * <p>
 * No se anota como {@code @Component} para que Spring Boot no lo registre dos veces: se
 * añade solo a la cadena de seguridad.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String PREFIJO_PROTEGIDO = "/api/auth/";
    private static final int MAXIMO_DE_CLIENTES_EN_MEMORIA = 10_000;
    private static final int LONGITUD_MAXIMA_EN_LOG = 200;

    /** Una IPv4 o IPv6 en texto; descarta valores arbitrarios que un cliente pudiera enviar. */
    private static final Pattern IP_VALIDA = Pattern.compile("^[0-9A-Fa-f:.]{3,45}$");

    private final RateLimitProperties propiedades;
    private final Cache<String, Bucket> cubetas;

    public RateLimitFilter(RateLimitProperties propiedades) {
        this.propiedades = propiedades;
        // Pasada una ventana sin actividad la cubeta ya estaría llena, así que se puede descartar.
        this.cubetas = Caffeine.newBuilder()
                .expireAfterAccess(propiedades.window())
                .maximumSize(MAXIMO_DE_CLIENTES_EN_MEMORIA)
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

        Bucket cubeta = cubetas.get(identificarCliente(request), clave -> nuevaCubeta());
        ConsumptionProbe resultado = cubeta.tryConsumeAndReturnRemaining(1);

        if (resultado.isConsumed()) {
            cadena.doFilter(request, response);
            return;
        }

        responderDemasiadosIntentos(response, TiempoDeEspera.enSegundos(resultado.getNanosToWaitForRefill()));
    }

    /**
     * Devuelve la clave de la cubeta: la IP de la cabecera configurada si existe y tiene
     * formato de IP; en cualquier otro caso, la IP de la conexión.
     */
    private String identificarCliente(HttpServletRequest request) {
        String clave = request.getRemoteAddr();

        String cabecera = propiedades.clientIpHeader();
        if (cabecera != null && !cabecera.isBlank()) {
            String valor = request.getHeader(cabecera);
            if (valor != null) {
                String primera = valor.split(",")[0].trim();
                if (IP_VALIDA.matcher(primera).matches()) {
                    clave = primera;
                }
            }
        }

        if (propiedades.debug()) {
            LOG.info("rate-limit clave={} remoteAddr={} X-Forwarded-For={} CF-Connecting-IP={} True-Client-IP={}",
                    clave,
                    request.getRemoteAddr(),
                    paraLog(request.getHeader("X-Forwarded-For")),
                    paraLog(request.getHeader("CF-Connecting-IP")),
                    paraLog(request.getHeader("True-Client-IP")));
        }
        return clave;
    }

    private Bucket nuevaCubeta() {
        return Bucket.builder()
                .addLimit(limite -> limite
                        .capacity(propiedades.capacity())
                        .refillGreedy(propiedades.capacity(), propiedades.window()))
                .build();
    }

    /** Recorta el valor de una cabecera controlada por el cliente antes de escribirlo en el log. */
    private static String paraLog(String valor) {
        if (valor == null) {
            return "-";
        }
        return valor.length() <= LONGITUD_MAXIMA_EN_LOG ? valor : valor.substring(0, LONGITUD_MAXIMA_EN_LOG) + "...";
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
