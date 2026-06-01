package com.example.springhotel.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service technique responsable de la creation et de la validation des JWT.
 * <p>
 * Boite a outils JWT de l'application. Cette classe ne connait rien de HTTP,
 * rien de Spring Security, rien de la base. Elle prend en entree un email +
 * des roles et te rend un token. Tu lui rends un token, elle te rend l'email
 * et les roles.
 * <p>
 * Pourquoi en faire un service separe :
 *   un, pour qu'il soit reutilisable dans le filtre ET dans le controller login,
 *   deux, pour qu'il soit testable independamment du reste de l'app,
 *   trois, pour qu'on puisse demain remplacer l'implementation custom par
 *   un fournisseur OAuth2/OIDC (Keycloak par exemple) en ne touchant que
 *   cette classe.
 * <p>
 * Algorithme utilise : HS256 (HMAC-SHA256). Choix volontaire pour ce projet :
 * un seul serveur emet ET valide les tokens, donc une cle symetrique partagee
 * suffit. Pour une architecture multi-services ou OIDC, on basculerait sur
 * RS256 (cle asymetrique) sans changer le reste de l'application.
 *
 * @see JwtProperties
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /**
     * Nom du claim qui porte l'id numerique de l'utilisateur en base.
     * On le met dans le token pour eviter une requete findByEmail() a chaque
     * appel authentifie.
     */
    public static final String CLAIM_USER_ID = "userId";

    /**
     * Nom du claim qui porte la liste des roles applicatifs.
     */
    public static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildSigningKey(properties.getSecret());
    }

    /**
     * Construit la cle HMAC a partir du secret de configuration.
     * <p>
     * On encode le secret en UTF-8 puis on demande a JJWT de fabriquer une
     * cle compatible HS256. JJWT verifiera lui-meme que la cle fait au moins
     * 256 bits et levera une exception au demarrage sinon.
     */
    private static SecretKey buildSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genere un nouveau JWT pour un utilisateur.
     *
     * @param email   email de l'utilisateur, pose en tant que sujet du token (claim sub)
     * @param userId  id numerique de l'utilisateur en base
     * @param roles   liste des roles applicatifs (par exemple ROLE_USER, ROLE_ADMIN)
     * @return le token signe, pret a etre renvoye au client
     */
    public String generateToken(String email, Long userId, List<String> roles) {
        long nowMillis = System.currentTimeMillis();
        Date issuedAt = new Date(nowMillis);
        Date expiration = new Date(nowMillis + properties.getExpirationMillis());

        return Jwts.builder()
                .subject(email)
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_ROLES, roles
                ))
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extrait l'email (claim sub) d'un token valide.
     *
     * @throws JwtException si le token est invalide
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrait l'id numerique de l'utilisateur (claim userId) d'un token valide.
     *
     * @throws JwtException si le token est invalide
     */
    public Long extractUserId(String token) {
        Object raw = parseClaims(token).get(CLAIM_USER_ID);
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Extrait la liste des roles (claim roles) d'un token valide.
     *
     * @throws JwtException si le token est invalide
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object raw = parseClaims(token).get(CLAIM_ROLES);
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    /**
     * Verifie qu'un token est syntaxiquement valide, signe avec notre cle, et
     * non expire.
     * <p>
     * Ne leve jamais d'exception : si le token est invalide pour n'importe
     * quelle raison, renvoie false. Pratique pour etre utilise comme garde
     * dans le filtre sans avoir a wrapper avec try/catch.
     *
     * @param token la chaine JWT a verifier, peut etre null ou vide
     * @return true si le token est valide, false sinon
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token JWT expire : {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse les claims d'un token apres avoir verifie sa signature et son
     * expiration. Methode privee, point d'entree unique pour toutes les
     * methodes d'extraction.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}