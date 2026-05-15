package com.example.springhotel.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre HTTP qui s'execute une fois par requete et qui transforme un en-tete
 * Authorization Bearer en authentification Spring Security.
 * <p>
 * Cycle de vie d'une requete authentifiee :
 *   un, le filtre est appele par la chaine Spring Security avant le controller,
 *   deux, il regarde le header Authorization,
 *   trois, s'il commence par "Bearer ", il extrait le token,
 *   quatre, il demande a JwtService de valider le token,
 *   cinq, s'il est valide, il pose un Authentication dans le SecurityContext,
 *   six, il passe la main au filtre suivant (qui finira par appeler le controller).
 * <p>
 * Si aucun header n'est present ou s'il est invalide, le filtre laisse passer
 * sans poser d'authentification. C'est ensuite SecurityConfig qui decide si
 * l'endpoint demande exigeait une authentification (renvoie 401) ou pas.
 * <p>
 * Important : ce filtre ne DECIDE PAS qui a le droit de faire quoi. Il se
 * contente de dire "voici qui est connecte". La distinction est la base d'une
 * bonne architecture de securite Spring.
 *
 * @see JwtService
 * @see com.example.springhotel.configuration.SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null && jwtService.isTokenValid(token)) {
            authenticateUser(request, token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Lit le header Authorization et renvoie le token nu, sans le prefixe.
     *
     * @return le token JWT, ou null si le header est absent ou mal forme
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Construit un Authentication a partir des claims du token et le pose
     * dans le SecurityContextHolder.
     */
    private void authenticateUser(HttpServletRequest request, String token) {
        try {
            String email = jwtService.extractEmail(token);
            List<String> roles = jwtService.extractRoles(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Utilisateur authentifie par JWT : {} (roles {})", email, roles);
        } catch (Exception e) {
            // Defensif : si l'extraction des claims echoue alors que le token semblait
            // valide, on ne propage pas l'erreur. La requete continuera sans authentification,
            // et Spring Security se chargera de renvoyer 401 si l'endpoint l'exigeait.
            log.warn("Echec inattendu de l'authentification JWT : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}