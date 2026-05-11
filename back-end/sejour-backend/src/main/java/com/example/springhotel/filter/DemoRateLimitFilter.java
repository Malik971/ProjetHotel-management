package com.example.springhotel.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtre HTTP qui limite le nombre de requetes par IP sur les endpoints sensibles
 * de l'application en mode demo publique.
 * <p>
 * <b>Pourquoi en interne et pas Bucket4j ?</b> Pour une demo portfolio sur free tier,
 * une dependance supplementaire (Bucket4j + son backend en memoire / Redis) est de
 * l'overkill. Une {@link ConcurrentHashMap} de seaux atomiques par IP, purgee
 * passivement a chaque acces, suffit largement et n'ajoute zero dependance.
 * <p>
 * <b>Algorithme :</b> fenetre glissante simple sur 60 secondes. Chaque IP a un
 * compteur. Quand l'IP fait une requete sur un endpoint protege :
 *   <ul>
 *     <li>Si la derniere requete date de plus de 60s, on reinitialise le compteur a 1.</li>
 *     <li>Sinon, on incremente. Au-dela de {@code maxRequests}, on retourne 429.</li>
 *   </ul>
 * <p>
 * <b>Endpoints proteges :</b> ecritures sensibles uniquement, pour ne pas penaliser
 * la navigation normale. La liste vit dans {@link #RATE_LIMITED_PATHS} ; les patterns
 * sont des prefixes simples.
 * <p>
 * <b>Activation :</b> propriete {@code demo.rate-limit.enabled}. En dev local, on
 * laisse a false pour ne pas s'embeter ; en prod, on passe a true.
 * <p>
 * <b>Limites assumees :</b>
 *   <ul>
 *     <li>Pas de persistance entre redemarrages : OK, c'est une defense anti-bot,
 *         pas un quota client.</li>
 *     <li>Pas de partage entre instances : OK, on tourne en mono-instance free tier.</li>
 *     <li>Identification par IP X-Forwarded-For : suffisant tant qu'on ne vise pas
 *         un attaquant determine.</li>
 *   </ul>
 */
@Component
public class DemoRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DemoRateLimitFilter.class);

    /**
     * Endpoints proteges : ecritures et appels couteux.
     * Les comparaisons se font par {@link String#startsWith}.
     */
    private static final Set<RateLimitedPath> RATE_LIMITED_PATHS = Set.of(
            new RateLimitedPath("POST", "/api/v1/register"),
            new RateLimitedPath("POST", "/api/v1/login"),
            new RateLimitedPath("POST", "/api/reservations"),
            new RateLimitedPath("POST", "/api/admin/pastell/poll")
    );

    /** Active le filtre uniquement si la propriete est a true. */
    @Value("${demo.rate-limit.enabled:false}")
    private boolean enabled;

    /** Limite par IP, par fenetre. */
    @Value("${demo.rate-limit.max-requests:10}")
    private int maxRequests;

    /** Largeur de la fenetre, en secondes. */
    @Value("${demo.rate-limit.window-seconds:60}")
    private int windowSeconds;

    /**
     * Compteurs par IP. ConcurrentHashMap pour la thread-safety, AtomicInteger
     * pour l'increment sans verrou.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || !isRateLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> new Bucket());
        boolean allowed = bucket.tryAcquire(maxRequests, windowSeconds);

        if (!allowed) {
            log.warn("Rate limit depasse pour IP {} sur {} {} (>{}/{}s).",
                    ip, request.getMethod(), request.getRequestURI(), maxRequests, windowSeconds);
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.getWriter().write(String.format(
                    "{\"error\":\"rate_limited\",\"limit\":%d,\"windowSeconds\":%d,"
                            + "\"hint\":\"Demo publique : ralentissez ou patientez %d secondes.\"}",
                    maxRequests, windowSeconds, windowSeconds));
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Verifie si la requete courante correspond a un endpoint sous rate limit.
     */
    private boolean isRateLimited(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        for (RateLimitedPath rlp : RATE_LIMITED_PATHS) {
            if (rlp.method.equalsIgnoreCase(method) && path.startsWith(rlp.pathPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extrait l'IP client, en respectant {@code X-Forwarded-For} si present
     * (Render et Netlify mettent l'IP reelle dans cet entete).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For: client, proxy1, proxy2... on prend le premier.
            int comma = xff.indexOf(',');
            return (comma >= 0) ? xff.substring(0, comma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Petit seau atomique par IP. Pas de purge active : si une IP devient
     * silencieuse, son bucket reste en memoire, mais a quelques dizaines
     * d'octets, c'est negligeable a l'echelle d'une demo.
     */
    private static final class Bucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();

        synchronized boolean tryAcquire(int maxRequests, int windowSeconds) {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).getSeconds() >= windowSeconds) {
                windowStart = now;
                count.set(1);
                return true;
            }
            if (count.get() >= maxRequests) {
                return false;
            }
            count.incrementAndGet();
            return true;
        }
    }

    /** Tuple immuable (methode HTTP, prefixe d'URL). */
    private record RateLimitedPath(String method, String pathPrefix) { }
}
