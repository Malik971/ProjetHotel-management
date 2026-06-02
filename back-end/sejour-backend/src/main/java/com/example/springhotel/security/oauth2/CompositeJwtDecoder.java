package com.example.springhotel.security.oauth2;

import com.example.springhotel.security.jwt.JwtService;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.text.ParseException;
import java.util.Map;

/**
 * Decodeur JWT composite qui couvre deux origines de token en coexistence :
 * <p>
 * Un, les tokens emis par Keycloak (claim "iss" present et egal a l'issuer
 * configure). Ces tokens sont signes en RS256 et valides via le JWKS de
 * Keycloak. C'est le flux OAuth2 Authorization Code PKCE.
 * <p>
 * Deux, les tokens emis par le LoginController maison (claim "iss" absent).
 * Ces tokens sont signes en HS256 via JwtService. C'est le flux de
 * compatibilite pour les comptes de demo existants.
 * <p>
 * Pourquoi un decodeur composite et pas deux filtres distincts :
 * Spring Security ne permet qu'un seul BearerTokenAuthenticationFilter par
 * chaine. Ce filtre appelle un seul JwtDecoder. On centralise donc la
 * logique de dispatch ici, en lisant le claim "iss" du payload base64 AVANT
 * toute verification de signature (lecture non-authenticating, juste pour
 * router). La verification de signature reste entierement deleguee a chaque
 * decodeur specialise.
 * <p>
 * Cote sejour-backend, rien ne change pour les controllers existants :
 * Authentication.getName() renvoie toujours l'email ou le sub selon l'origine
 * du token, et les roles sont toujours sous la forme ROLE_* grace au
 * KeycloakJwtAuthenticationConverter.
 *
 * @see KeycloakJwtAuthenticationConverter
 * @see com.example.springhotel.security.jwt.JwtService
 * @see com.example.springhotel.configuration.SecurityConfig
 */
public class CompositeJwtDecoder implements JwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(CompositeJwtDecoder.class);

    /** Claim standard OpenID Connect qui identifie l'emetteur du token. */
    private static final String CLAIM_ISSUER = "iss";

    /**
     * Decodeur delegue pour les tokens Keycloak.
     * Instancie par NimbusJwtDecoder.withIssuerLocation() qui telecharge
     * automatiquement le JWKS de Keycloak au demarrage.
     */
    private final NimbusJwtDecoder keycloakDecoder;

    /**
     * Service JWT maison pour valider les tokens signes en HS256.
     * Conserve de l'implementation precedente, on ne le remplace pas.
     */
    private final JwtService jwtService;

    /**
     * Valeur exacte attendue dans le claim "iss" des tokens Keycloak.
     * Doit correspondre a spring.security.oauth2.resourceserver.jwt.issuer-uri.
     */
    private final String keycloakIssuer;

    /**
     * @param keycloakDecoder decodeur Nimbus configure avec le JWKS Keycloak
     * @param jwtService      service JWT maison (validation HS256)
     * @param keycloakIssuer  valeur attendue du claim iss pour les tokens Keycloak
     */
    public CompositeJwtDecoder(
            NimbusJwtDecoder keycloakDecoder,
            JwtService jwtService,
            String keycloakIssuer
    ) {
        this.keycloakDecoder = keycloakDecoder;
        this.jwtService = jwtService;
        this.keycloakIssuer = keycloakIssuer;
    }

    /**
     * Point d'entree unique appele par BearerTokenAuthenticationFilter.
     * <p>
     * Cycle de dispatch :
     *   un, on lit le claim "iss" du payload base64 sans verifier la signature,
     *   deux, si "iss" correspond a l'issuer Keycloak, on delege au decodeur Keycloak,
     *   trois, sinon on delege au decodeur maison (HS256).
     *
     * @param token la chaine JWT brute extraite du header Authorization
     * @return un Jwt valide avec ses claims
     * @throws JwtException si le token est invalide selon le decodeur choisi
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = extractIssuerWithoutVerification(token);

        if (keycloakIssuer.equals(issuer)) {
            log.debug("Token Keycloak detecte (iss={}), validation RS256 via JWKS.", issuer);
            return keycloakDecoder.decode(token);
        }

        log.debug("Token maison detecte (iss absent ou different), validation HS256.");
        return decodeHomeMadeToken(token);
    }

    /**
     * Lit le claim "iss" du payload base64 SANS verifier la signature.
     * <p>
     * C'est intentionnel : on ne fait que router, pas authentifier. La
     * verification de signature est entierement deleguee au decodeur choisi.
     * Un attaquant ne peut pas profiter de cette lecture non-verificatrice
     * pour injecter un token arbitraire : la verification arrive juste apres.
     *
     * @return la valeur du claim "iss", ou null si absent ou si le parsing echoue
     */
    private String extractIssuerWithoutVerification(String token) {
        try {
            Map<String, Object> claims = JWTParser.parse(token).getJWTClaimsSet().getClaims();
            Object iss = claims.get(CLAIM_ISSUER);
            return iss != null ? iss.toString() : null;
        } catch (ParseException e) {
            log.debug("Impossible de parser le claim iss (token malform ou vide) : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Valide un token JWT maison via JwtService (HS256) et le convertit
     * en objet Jwt Spring Security.
     * <p>
     * On reconstruit manuellement un Jwt a partir des claims extraits par
     * JwtService. Spring Security attend un Jwt avec au moins les claims
     * "sub" et "exp" pour construire l'Authentication correctement.
     *
     * On leve BadJwtException (et non JwtException generique) pour que Spring
     * Security la convertisse en InvalidBearerTokenException, donc en reponse
     * HTTP 401 propre. Une JwtException generique serait traitee comme une
     * AuthenticationServiceException, soit une erreur serveur.
     *
     * @throws BadJwtException si le token est invalide, expire ou mal signe
     */
    private Jwt decodeHomeMadeToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new BadJwtException("Token JWT maison invalide ou expire.");
        }

        try {
            // On parse une seconde fois pour extraire tous les claims.
            // JwtService a deja valide la signature au-dessus, ce parsing
            // ne fait que lire les donnees.
            com.nimbusds.jwt.JWT parsed = JWTParser.parse(token);
            Map<String, Object> claims = parsed.getJWTClaimsSet().getClaims();

            // Extraction des dates pour construire le Jwt Spring
            java.util.Date issuedAt = (java.util.Date) claims.get("iat");
            java.util.Date expiresAt = (java.util.Date) claims.get("exp");

            java.time.Instant iat = issuedAt != null
                    ? issuedAt.toInstant()
                    : java.time.Instant.now();
            java.time.Instant exp = expiresAt != null
                    ? expiresAt.toInstant()
                    : java.time.Instant.now().plusSeconds(86400);

            return Jwt.withTokenValue(token)
                    .headers(h -> {
                        try {
                            h.putAll(parsed.getHeader().toJSONObject());
                        } catch (Exception ignore) {
                            h.put("alg", "HS256");
                        }
                    })
                    .claims(c -> c.putAll(claims))
                    .issuedAt(iat)
                    .expiresAt(exp)
                    .build();

        } catch (ParseException e) {
            throw new BadJwtException("Echec du parsing du token JWT maison : " + e.getMessage(), e);
        }
    }
}