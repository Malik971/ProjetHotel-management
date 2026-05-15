package com.example.springhotel.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration typee du module JWT, lue depuis application.properties.
 * <p>
 * Pourquoi ce fichier existe : centraliser la lecture de la config JWT en un
 * seul endroit. Au lieu d'avoir des @Value eparpilles dans JwtService et
 * JwtAuthenticationFilter, on a une source de verite unique. Si demain on
 * ajoute jwt.issuer ou jwt.audience, on l'ajoute ici et c'est tout.
 * <p>
 * Pattern Spring Boot standard : @ConfigurationProperties typee + validation
 * Bean Validation au demarrage. Si JWT_SECRET est absent en prod, l'app
 * refuse de demarrer (fail-fast) plutot que d'exposer un endpoint non-securise.
 * <p>
 * Les properties associees dans application.properties :
 * <pre>
 * jwt.secret=${JWT_SECRET}
 * jwt.expiration-millis=86400000
 * </pre>
 *
 * @see JwtService
 * @see JwtAuthenticationFilter
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /**
     * Cle secrete utilisee pour signer et verifier les JWT.
     * <p>
     * HS256 exige une cle d'au moins 256 bits soit 32 caracteres ASCII.
     * En pratique on recommande 64 caracteres aleatoires pour plus de robustesse.
     * <p>
     * IMPORTANT : ne jamais committer cette cle dans le code. Elle est lue
     * depuis la variable d'environnement JWT_SECRET.
     */
    @NotBlank(message = "jwt.secret doit etre defini (variable d'environnement JWT_SECRET)")
    private String secret;

    /**
     * Duree de validite d'un token, en millisecondes.
     * <p>
     * Defaut 86400000 ms = 24 heures. Compromis entre experience utilisateur
     * (on ne se reloggue pas toutes les heures) et securite (un token vole
     * expire dans la journee).
     */
    @Positive(message = "jwt.expiration-millis doit etre strictement positif")
    private long expirationMillis = 86_400_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }
}