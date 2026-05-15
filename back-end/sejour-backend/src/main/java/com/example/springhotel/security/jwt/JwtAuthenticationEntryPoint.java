package com.example.springhotel.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Produit une reponse JSON propre quand un client non-authentifie tape un
 * endpoint protege.
 * <p>
 * Sans ce composant, Spring Security renvoie par defaut une page HTML
 * d'erreur ou un 401 vide. Or notre front React veut du JSON pour pouvoir
 * afficher un toast lisible. On uniformise donc la reponse 401 sur le format
 * habituel :
 * <pre>
 * {
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "message": "Authentification requise",
 *   "timestamp": "2026-05-14T10:23:00Z"
 * }
 * </pre>
 * <p>
 * Note d'implementation : on cree notre propre ObjectMapper en interne plutot
 * que de l'injecter depuis le contexte Spring. Raison : avec Spring Boot 4.0
 * et le starter webmvc, l'auto-configuration du bean ObjectMapper peut etre
 * tardive ou conditionnelle. Comme on l'utilise uniquement ici pour serialiser
 * un petit objet d'erreur, instancier un ObjectMapper local n'a aucun cout
 * fonctionnel et evite toute fragilite liee a l'ordre des autoconfigs.
 *
 * @see com.example.springhotel.configuration.SecurityConfig
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * ObjectMapper interne, instancie une fois, thread-safe par construction
     * de Jackson. On n'injecte pas le bean Spring pour ne pas dependre de
     * l'ordre d'auto-configuration.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", "Authentification requise",
                "timestamp", Instant.now().toString()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}